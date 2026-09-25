package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.orgzly.android.BookFormat
import com.orgzly.android.LocalStorage
import com.orgzly.android.NotesOrgExporter
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.DbRepoBookRepository
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskOrderValues
import com.orgzly.android.ui.tasks.model.TaskOrdering
import com.orgzly.android.ui.tasks.model.toTask
import com.orgzly.android.util.MiscUtils
import com.orgzly.org.OrgProperties
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The hand-picked order is a property on the note, which means it lives in the org file.
 *
 * That is the whole reason it is stored there rather than in Orgzly's own preferences: loading
 * a notebook from a repo deletes every note in it and re-inserts
 * (DataRepository.loadBookFromReader), so a sync that pulls a notebook edited elsewhere hands
 * every note a new id, and anything kept locally against those ids would be scrambled. These
 * run that for real against a database instead of asserting against a model of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskOrderDurabilityTest {

    private lateinit var context: Context
    private lateinit var dataRepository: DataRepository
    private lateinit var database: OrgzlyDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = OrgzlyDatabase.forMemory(context)

        dataRepository = DataRepository(
            context,
            database,
            RepoFactory(context, DbRepoBookRepository(database)),
            context.resources,
            LocalStorage(context),
        )

        AppPreferences.states(context, "TODO | DONE")
    }

    @After
    fun tearDown() = database.close()

    private fun loadBook(content: String) {
        val tmpFile = dataRepository.getTempBookFile()
        try {
            MiscUtils.writeStringToFile(content, tmpFile)
            dataRepository.loadBookFromFile("tasks", BookFormat.ORG, tmpFile, null)
        } finally {
            tmpFile.delete()
        }
    }

    private fun tasks(): List<Task> {
        val doneKeywords = AppPreferences.doneKeywordsSet(context)
        val orders = readTaskOrders(database)

        return dataRepository
            .selectNotesFromQuery(InternalQueryParser().parse(TasksViewModel.QUERY))
            .map { it.toTask(doneKeywords, orders) }
    }

    /** The same write TasksActions.setOrder makes, without needing Dagger for the use case. */
    private fun setOrder(noteId: Long, value: Long) {
        dataRepository.insertNoteProperties(
            noteId,
            OrgProperties().apply { set(TaskOrderValues.PROPERTY, value.toString()) },
        )
    }

    private val original = """
        * TODO Call the bank
        * TODO Book flights
        * TODO Renew passport
    """.trimIndent()

    @Test
    fun `the order reaches the org file`() {
        loadBook(original)

        val note = dataRepository.getLastNote("Call the bank")!!
        setOrder(note.id, 1024)

        val exported = NotesOrgExporter(dataRepository).exportNote(note.id)

        assertTrue(
            "expected the order property in:\n$exported",
            exported.contains(":${TaskOrderValues.PROPERTY}: 1024"),
        )
    }

    @Test
    fun `a hand-picked order survives every note id being reminted by a reload`() {
        loadBook(original)

        val before = tasks()
        assertEquals(3, before.size)

        // Drag: reverse the day.
        before.reversed().forEachIndexed { index, task ->
            setOrder(task.noteId, (index + 1) * TaskOrderValues.STRIDE)
        }

        // Round-trip through the file, exactly as a sync would: export what is in the
        // database, then load it back as though it had arrived from the repo.
        val exported = tasks().joinToString("\n") {
            NotesOrgExporter(dataRepository).exportNote(it.noteId).trimEnd()
        }

        loadBook(exported)

        val after = tasks()

        assertNotEquals(
            "reload should have reminted the note ids, or this test proves nothing",
            before.map { it.noteId }.toSet(),
            after.map { it.noteId }.toSet(),
        )

        assertEquals(
            listOf("Renew passport", "Book flights", "Call the bank"),
            TaskOrdering.sort(after).map { it.title },
        )
    }

    /**
     * Renaming a task elsewhere used to cost it its place, back when the order was matched by
     * title. Carried in the file, the order is simply part of the note.
     */
    @Test
    fun `renaming a task outside Orgzly keeps its place`() {
        loadBook(
            """
            * TODO Call the bank
              :PROPERTIES:
              :${TaskOrderValues.PROPERTY}: 3072
              :END:
            * TODO Book flights
              :PROPERTIES:
              :${TaskOrderValues.PROPERTY}: 2048
              :END:
            """.trimIndent()
        )

        assertEquals(
            listOf("Book flights", "Call the bank"),
            TaskOrdering.sort(tasks()).map { it.title },
        )

        loadBook(
            """
            * TODO Call the credit union
              :PROPERTIES:
              :${TaskOrderValues.PROPERTY}: 3072
              :END:
            * TODO Book flights
              :PROPERTIES:
              :${TaskOrderValues.PROPERTY}: 2048
              :END:
            """.trimIndent()
        )

        assertEquals(
            listOf("Book flights", "Call the credit union"),
            TaskOrdering.sort(tasks()).map { it.title },
        )
    }

    @Test
    fun `tasks without the property are unaffected`() {
        loadBook(original)

        assertEquals(3, tasks().count { it.order == null })
    }
}
