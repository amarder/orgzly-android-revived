package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.orgzly.android.LocalStorage
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.DbRepoBookRepository
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.ui.tasks.model.TaskDate
import com.orgzly.org.datetime.OrgRange
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Covers the payload a quick-added task is built from. Both fields here are overridden
 * rather than inherited from NoteBuilder, and getting either wrong produces a task that is
 * invisible in the list or buried under "No date".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TasksActionsTest {

    private lateinit var context: Context
    private lateinit var database: OrgzlyDatabase
    private lateinit var actions: TasksActions

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = OrgzlyDatabase.forMemory(context)

        val dataRepository = DataRepository(
            context,
            database,
            RepoFactory(context, DbRepoBookRepository(database)),
            context.resources,
            LocalStorage(context),
        )

        AppPreferences.states(context, "TODO NEXT | DONE")
        actions = TasksActions(dataRepository, context)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a new task is scheduled for today`() {
        val scheduled = actions.newTaskPayload("Buy milk").scheduled
        assertNotNull(scheduled)

        val start = OrgRange.parse(scheduled).startTime.calendar
        val today = Calendar.getInstance()

        assertEquals(today.get(Calendar.YEAR), start.get(Calendar.YEAR))
        assertEquals(today.get(Calendar.MONTH), start.get(Calendar.MONTH))
        assertEquals(today.get(Calendar.DAY_OF_MONTH), start.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `the scheduled timestamp is active, so it reaches the agenda`() {
        val scheduled = actions.newTaskPayload("Buy milk").scheduled
        assertEquals(true, OrgRange.parse(scheduled).startTime.isActive)
    }

    @Test
    fun `a new task gets a to-do state even when the new-note preference says otherwise`() {
        AppPreferences.newNoteState(context, "NOTE")

        assertEquals("TODO", actions.newTaskPayload("Buy milk").state)
    }

    @Test
    fun `the to-do state follows the user's own workflow`() {
        AppPreferences.states(context, "NEXT LATER | DONE")

        assertEquals("NEXT", actions.newTaskPayload("Buy milk").state)
    }

    @Test
    fun `the title is carried through`() {
        assertEquals("Buy milk", actions.newTaskPayload("Buy milk").title)
    }

    // --- the new-task screen's four fields ----------------------------------------------

    @Test
    fun `a chosen date is what gets scheduled, not today`() {
        val payload = actions.newTaskPayload("Buy milk", TaskDate(2027, 0, 9))

        assertEquals("2027-01-09", OrgRange.parse(payload.scheduled).startTime.calendar.let {
            "%04d-%02d-%02d".format(
                it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH)
            )
        })
    }

    /** Clearing the date has to mean no SCHEDULED at all, not a fallback to today. */
    @Test
    fun `no date means no scheduled timestamp`() {
        assertNull(actions.newTaskPayload("Buy milk", date = null).scheduled)
    }

    @Test
    fun `a task with no date is still a to-do`() {
        assertEquals("TODO", actions.newTaskPayload("Buy milk", date = null).state)
    }

    /**
     * Pasting a list that turns out to be empty must not reach the database at all - not even
     * to resolve a target notebook, which would create the default one as a side effect.
     */
    @Test
    fun `creating from blank lines creates nothing`() {
        assertEquals(0, actions.createMany(emptyList(), null))
        assertEquals(0, actions.createMany(listOf("", "   ", "\n"), null))
    }
}
