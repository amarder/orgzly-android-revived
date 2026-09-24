package com.orgzly.android.ui.tasks.model

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The list is flat and unheaded, so this ordering is the only cue about what is urgent.
 * Plain JUnit - nothing here needs Android.
 */
class TaskOrderingTest {

    private val zone: DateTimeZone = DateTimeZone.forID("Europe/London")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = DateTime(y, m, d, h, 0, zone).millis

    private fun task(
        id: Long = 1,
        title: String = "t",
        book: String = "b",
        priority: String? = null,
        scheduled: Long? = null,
        deadline: Long? = null,
    ) = Task(
        noteId = id,
        bookId = 1,
        bookName = book,
        title = title,
        state = "TODO",
        isDone = false,
        priority = priority,
        tags = emptyList(),
        hasContent = false,
        scheduledMillis = scheduled,
        scheduledRangeString = null,
        deadlineMillis = deadline,
        deadlineRangeString = null,
    )

    private fun order(vararg tasks: Task) = TaskOrdering.sort(tasks.toList()).map { it.title }

    // --- which date counts ------------------------------------------------------------

    @Test
    fun `the earlier of scheduled and deadline is the due date`() {
        assertEquals(
            at(2026, 9, 23),
            task(scheduled = at(2026, 10, 30), deadline = at(2026, 9, 23)).dueMillis,
        )
        assertEquals(
            at(2026, 9, 24),
            task(scheduled = at(2026, 9, 24), deadline = at(2026, 12, 1)).dueMillis,
        )
    }

    @Test
    fun `a task with neither date has no due date`() {
        assertNull(task().dueMillis)
    }

    // --- ordering ---------------------------------------------------------------------

    @Test
    fun `soonest first`() {
        assertEquals(
            listOf("yesterday", "today", "next week"),
            order(
                task(id = 1, title = "next week", scheduled = at(2026, 10, 1)),
                task(id = 2, title = "yesterday", scheduled = at(2026, 9, 23)),
                task(id = 3, title = "today", scheduled = at(2026, 9, 24)),
            ),
        )
    }

    @Test
    fun `undated tasks sort after everything with a date`() {
        assertEquals(
            listOf("far future", "undated"),
            order(
                task(id = 1, title = "undated"),
                task(id = 2, title = "far future", scheduled = at(2099, 1, 1)),
            ),
        )
    }

    @Test
    fun `a deadline is ranked against other tasks' scheduled dates`() {
        assertEquals(
            listOf("deadline soon", "scheduled later"),
            order(
                task(id = 1, title = "scheduled later", scheduled = at(2026, 10, 5)),
                task(id = 2, title = "deadline soon", deadline = at(2026, 9, 25)),
            ),
        )
    }

    @Test
    fun `same date falls back to priority, then notebook, then title`() {
        val day = at(2026, 9, 24)

        assertEquals(
            listOf("apple", "mango", "zebra", "berry"),
            order(
                task(id = 1, title = "zebra", book = "a", priority = "C", scheduled = day),
                task(id = 2, title = "apple", book = "b", priority = "A", scheduled = day),
                task(id = 3, title = "mango", book = "a", priority = "C", scheduled = day),
                task(id = 4, title = "berry", book = "a", priority = null, scheduled = day),
            ),
        )
    }

    @Test
    fun `completing a task does not move it`() {
        val day = at(2026, 9, 24)

        val before = TaskOrdering.sort(
            listOf(
                task(id = 1, title = "one", scheduled = day),
                task(id = 2, title = "two", scheduled = day),
                task(id = 3, title = "three", scheduled = day),
            )
        )

        // Same tasks, but the middle one is now done. State is not a sort key, so the row
        // must stay put - that is what stops it jumping when the circle is tapped.
        val after = TaskOrdering.sort(
            before.map { if (it.noteId == 2L) it.copy(state = "DONE", isDone = true) else it }
        )

        assertEquals(before.map { it.noteId }, after.map { it.noteId })
    }

    @Test
    fun `an empty list sorts to an empty list`() {
        assertEquals(emptyList<Task>(), TaskOrdering.sort(emptyList()))
    }
}
