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
        order: Long? = null,
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
        order = order,
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

    // --- hand-picked order ---------------------------------------------------------

    @Test
    fun `a dragged order overrides priority and title within a day`() {
        val day = at(2026, 9, 24)

        assertEquals(
            listOf("zebra", "apple"),
            order(
                task(id = 1, title = "apple", priority = "A", scheduled = day, order = 2048),
                task(id = 2, title = "zebra", priority = "C", scheduled = day, order = 1024),
            ),
        )
    }

    /**
     * The first sort key is the day, not the timestamp, precisely so this works: a task with
     * a clock time can be dragged above an untimed one due the same day.
     */
    @Test
    fun `a dragged order overrides time of day`() {
        assertEquals(
            listOf("evening", "morning"),
            order(
                task(id = 1, title = "morning", scheduled = at(2026, 9, 24, h = 9), order = 2048),
                task(id = 2, title = "evening", scheduled = at(2026, 9, 24, h = 20), order = 1024),
            ),
        )
    }

    @Test
    fun `an order cannot lift a task onto another day`() {
        assertEquals(
            listOf("today", "tomorrow"),
            order(
                task(id = 1, title = "tomorrow", scheduled = at(2026, 9, 25), order = 1),
                task(id = 2, title = "today", scheduled = at(2026, 9, 24), order = 9999),
            ),
        )
    }

    @Test
    fun `undated tasks stay last however they are ordered`() {
        assertEquals(
            listOf("dated", "undated"),
            order(
                task(id = 1, title = "undated", order = 1),
                task(id = 2, title = "dated", scheduled = at(2099, 1, 1), order = 9999),
            ),
        )
    }

    /** A task never dragged has no order, and sorts below everything that has one. */
    @Test
    fun `unordered tasks sort after ordered ones on the same day`() {
        val day = at(2026, 9, 24)

        assertEquals(
            listOf("zebra", "apple"),
            order(
                task(id = 1, title = "apple", scheduled = day),
                task(id = 2, title = "zebra", scheduled = day, order = 1024),
            ),
        )
    }

    @Test
    fun `undated tasks can be ordered among themselves`() {
        assertEquals(
            listOf("zebra", "apple"),
            order(
                task(id = 1, title = "apple", order = 2048),
                task(id = 2, title = "zebra", order = 1024),
            ),
        )
    }

    // --- the day a drag is confined to -----------------------------------------------

    /**
     * Found on a device: a task dragged hard downwards escaped its own day and landed among
     * tomorrow's. The clamp is only as good as this range.
     */
    @Test
    fun `a day's range covers exactly the rows sharing its date`() {
        val tasks = listOf(
            task(id = 1, scheduled = at(2026, 9, 24)),
            task(id = 2, scheduled = at(2026, 9, 24, h = 18)),
            task(id = 3, scheduled = at(2026, 9, 25)),
            task(id = 4, scheduled = at(2026, 9, 25)),
            task(id = 5),
        )

        assertEquals(0..1, TaskOrdering.dayRange(tasks, 0))
        assertEquals(0..1, TaskOrdering.dayRange(tasks, 1))
        assertEquals(2..3, TaskOrdering.dayRange(tasks, 2))
        assertEquals(2..3, TaskOrdering.dayRange(tasks, 3))
    }

    /** Undated tasks are a day of their own, and can be ordered among themselves. */
    @Test
    fun `undated tasks form their own range`() {
        val tasks = listOf(
            task(id = 1, scheduled = at(2026, 9, 24)),
            task(id = 2),
            task(id = 3),
        )

        assertEquals(1..2, TaskOrdering.dayRange(tasks, 1))
    }

    @Test
    fun `a lone task on its date is a range of one`() {
        val tasks = listOf(
            task(id = 1, scheduled = at(2026, 9, 24)),
            task(id = 2, scheduled = at(2026, 9, 25)),
            task(id = 3, scheduled = at(2026, 9, 26)),
        )

        assertEquals(1..1, TaskOrdering.dayRange(tasks, 1))
    }

    @Test
    fun `a single-task list is its own range`() {
        assertEquals(0..0, TaskOrdering.dayRange(listOf(task(id = 1)), 0))
    }

    @Test
    fun `negative order values sort before positive ones`() {
        val day = at(2026, 9, 24)

        assertEquals(
            listOf("first", "second"),
            order(
                task(id = 1, title = "second", scheduled = day, order = 0),
                task(id = 2, title = "first", scheduled = day, order = -1024),
            ),
        )
    }
}
