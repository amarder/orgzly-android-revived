package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.orgzly.android.BookFormat
import com.orgzly.android.LocalStorage
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.DbRepoBookRepository
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.util.MiscUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the query string the tasks list actually runs, against a real database.
 *
 * The query is a string parsed at runtime, so a typo or a syntax assumption that turns out to
 * be wrong fails silently as an empty or over-broad list rather than a compile error.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TasksQueryTest {

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

        AppPreferences.states(context, "TODO NEXT | DONE CANCELLED")
        AppPreferences.defaultPriority(context, "B")
    }

    @After
    fun tearDown() = database.close()

    private fun setupBook(name: String, content: String) {
        val tmpFile = dataRepository.getTempBookFile()
        try {
            MiscUtils.writeStringToFile(content, tmpFile)
            dataRepository.loadBookFromFile(name, BookFormat.ORG, tmpFile, null)
        } finally {
            tmpFile.delete()
        }
    }

    private fun run(query: String): List<NoteView> =
        dataRepository.selectNotesFromQuery(InternalQueryParser().parse(query))

    private fun titles(query: String) = run(query).map { it.note.title }.sorted()

    @Test
    fun `includes both to-do and done states, and no plain headings`() {
        setupBook("book", """
            * TODO Open task
            * DONE Finished task
            * NEXT Next task
            * CANCELLED Abandoned task
            * Just a heading
            ** Nested plain heading
        """.trimIndent())

        assertEquals(
            listOf("Abandoned task", "Finished task", "Next task", "Open task"),
            titles(TasksViewModel.QUERY),
        )
    }

    @Test
    fun `honours the user's own keyword configuration`() {
        AppPreferences.states(context, "TODO WAITING | DONE")

        setupBook("book", """
            * WAITING Waiting on someone
            * NEXT Not a keyword any more
        """.trimIndent())

        // NEXT is no longer configured, so it is a plain heading with a word in front of it.
        assertEquals(listOf("Waiting on someone"), titles(TasksViewModel.QUERY))
    }

    @Test
    fun `excludes archived notes and everything under them`() {
        setupBook("book", """
            * TODO Visible task
            * TODO Archived parent                                          :ARCHIVE:
            ** TODO Child of archived parent
            * DONE Archived and done                                        :ARCHIVE:
        """.trimIndent())

        assertEquals(listOf("Visible task"), titles(TasksViewModel.QUERY))
    }

    @Test
    fun `the archived variant of the query includes them again`() {
        setupBook("book", """
            * TODO Visible task
            * TODO Archived task                                            :ARCHIVE:
        """.trimIndent())

        assertEquals(
            listOf("Archived task", "Visible task"),
            titles(TasksViewModel.QUERY_INCLUDING_ARCHIVED),
        )
    }

    @Test
    fun `spans every notebook`() {
        setupBook("work", "* TODO Work task")
        setupBook("home", "* DONE Home task")

        assertEquals(listOf("Home task", "Work task"), titles(TasksViewModel.QUERY))
    }
}
