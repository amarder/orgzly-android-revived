package com.orgzly.android.ui.tasks.model

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JUnit - nothing here needs Android. */
class TaskListEntriesTest {

    private val zone: DateTimeZone = DateTimeZone.forID("Europe/London")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = DateTime(y, m, d, h, 0, zone).millis

    private fun task(id: Long, scheduled: Long? = null) = Task(
        noteId = id,
        bookId = 1,
        bookName = "b",
        title = "t$id",
        state = "TODO",
        isDone = false,
        priority = null,
        tags = emptyList(),
        hasContent = false,
        scheduledMillis = scheduled,
        scheduledRangeString = null,
        deadlineMillis = null,
        deadlineRangeString = null,
    )

    @Test
    fun `an empty list produces no entries`() {
        assertEquals(emptyList<TaskListEntry>(), TaskListEntries.build(emptyList()))
    }

    @Test
    fun `a single day of tasks gets one header and a row per task, in order`() {
        val day = at(2026, 9, 24)
        val tasks = listOf(
            task(id = 1, scheduled = day),
            task(id = 2, scheduled = at(2026, 9, 24, h = 18)),
        )

        assertEquals(
            listOf(
                TaskListEntry.Header(TaskOrdering.dayBucket(day)),
                TaskListEntry.Row(tasks[0], 0),
                TaskListEntry.Row(tasks[1], 1),
            ),
            TaskListEntries.build(tasks),
        )
    }

    @Test
    fun `a new header appears at every day boundary, and nowhere else`() {
        val tasks = listOf(
            task(id = 1, scheduled = at(2026, 9, 24)),
            task(id = 2, scheduled = at(2026, 9, 24, h = 20)),
            task(id = 3, scheduled = at(2026, 9, 25)),
        )

        val entries = TaskListEntries.build(tasks)

        assertEquals(
            listOf(
                TaskListEntry.Header(TaskOrdering.dayBucket(tasks[0].dueMillis)),
                TaskListEntry.Row(tasks[0], 0),
                TaskListEntry.Row(tasks[1], 1),
                TaskListEntry.Header(TaskOrdering.dayBucket(tasks[2].dueMillis)),
                TaskListEntry.Row(tasks[2], 2),
            ),
            entries,
        )
    }

    @Test
    fun `an undated tail gets one null-bucket header right before it`() {
        val tasks = listOf(
            task(id = 1, scheduled = at(2026, 9, 24)),
            task(id = 2),
            task(id = 3),
        )

        assertEquals(
            listOf(
                TaskListEntry.Header(TaskOrdering.dayBucket(tasks[0].dueMillis)),
                TaskListEntry.Row(tasks[0], 0),
                TaskListEntry.Header(null),
                TaskListEntry.Row(tasks[1], 1),
                TaskListEntry.Row(tasks[2], 2),
            ),
            TaskListEntries.build(tasks),
        )
    }

    @Test
    fun `taskIndex always matches the task's position in the input list`() {
        val tasks = listOf(
            task(id = 1, scheduled = at(2026, 9, 24)),
            task(id = 2, scheduled = at(2026, 9, 25)),
            task(id = 3),
        )

        TaskListEntries.build(tasks).filterIsInstance<TaskListEntry.Row>().forEach { row ->
            assertEquals(tasks.indexOf(row.task), row.taskIndex)
        }
    }
}
