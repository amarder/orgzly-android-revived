package com.orgzly.android.ui.tasks.model

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plain JUnit - no Robolectric. TaskGrouping takes "now" and the zone as parameters precisely
 * so the boundary cases can be pinned down without an Android runtime.
 */
class TaskGroupingTest {

    private val zone: DateTimeZone = DateTimeZone.forID("Europe/London")

    /** Thu 24 Sep 2026, 14:30 local. */
    private val now = DateTime(2026, 9, 24, 14, 30, zone).millis

    private fun at(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0) =
        DateTime(y, m, d, h, min, zone).millis

    private fun bucket(dueMillis: Long?) = TaskGrouping.bucketOf(dueMillis, now, zone)

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

    // --- bucketOf ---------------------------------------------------------------------

    @Test
    fun `no date goes to NO_DATE`() {
        assertEquals(TaskBucket.NO_DATE, bucket(null))
    }

    @Test
    fun `yesterday is overdue`() {
        assertEquals(TaskBucket.OVERDUE, bucket(at(2026, 9, 23)))
    }

    @Test
    fun `earlier the same day is still today, not overdue`() {
        // 09:00 is before "now" (14:30) but after today's start, so it must not be OVERDUE.
        assertEquals(TaskBucket.TODAY, bucket(at(2026, 9, 24, 9, 0)))
    }

    @Test
    fun `midnight today is the first instant of TODAY`() {
        assertEquals(TaskBucket.TODAY, bucket(at(2026, 9, 24, 0, 0)))
    }

    @Test
    fun `one millisecond before midnight is still overdue`() {
        assertEquals(TaskBucket.OVERDUE, bucket(at(2026, 9, 24, 0, 0) - 1))
    }

    @Test
    fun `tomorrow is upcoming`() {
        assertEquals(TaskBucket.UPCOMING, bucket(at(2026, 9, 25)))
    }

    @Test
    fun `last instant before the upcoming window closes is upcoming`() {
        val windowEnd = DateTime(now, zone)
            .withTimeAtStartOfDay()
            .plusDays(TaskGrouping.UPCOMING_DAYS)
            .millis
        assertEquals(TaskBucket.UPCOMING, bucket(windowEnd - 1))
    }

    @Test
    fun `exactly the end of the upcoming window is later`() {
        val windowEnd = DateTime(now, zone)
            .withTimeAtStartOfDay()
            .plusDays(TaskGrouping.UPCOMING_DAYS)
            .millis
        assertEquals(TaskBucket.LATER, bucket(windowEnd))
    }

    // --- effective due date -----------------------------------------------------------

    @Test
    fun `earlier of scheduled and deadline decides the bucket`() {
        val t = task(scheduled = at(2026, 10, 30), deadline = at(2026, 9, 23))
        assertEquals(at(2026, 9, 23), t.due?.millis)
        assertEquals(DueKind.DEADLINE, t.due?.kind)
        assertEquals(TaskBucket.OVERDUE, bucket(t.due?.millis))
    }

    @Test
    fun `scheduled wins when it is the earlier of the two`() {
        val t = task(scheduled = at(2026, 9, 24), deadline = at(2026, 12, 1))
        assertEquals(DueKind.SCHEDULED, t.due?.kind)
    }

    @Test
    fun `deadline wins a tie because it is the harder commitment`() {
        val same = at(2026, 9, 24)
        assertEquals(DueKind.DEADLINE, task(scheduled = same, deadline = same).due?.kind)
    }

    // --- grouping ---------------------------------------------------------------------

    @Test
    fun `sections come back in bucket order and empty ones are dropped`() {
        val sections = TaskGrouping.group(
            listOf(
                task(id = 1, scheduled = at(2026, 12, 1)),   // LATER
                task(id = 2, scheduled = at(2026, 9, 23)),   // OVERDUE
                task(id = 3),                                // NO_DATE
            ),
            now,
            zone,
        )

        assertEquals(
            listOf(TaskBucket.OVERDUE, TaskBucket.LATER, TaskBucket.NO_DATE),
            sections.map { it.bucket },
        )
    }

    @Test
    fun `within a bucket the soonest task comes first`() {
        val sections = TaskGrouping.group(
            listOf(
                task(id = 1, title = "later today", scheduled = at(2026, 9, 24, 18, 0)),
                task(id = 2, title = "this morning", scheduled = at(2026, 9, 24, 8, 0)),
            ),
            now,
            zone,
        )

        assertEquals(listOf("this morning", "later today"), sections.single().tasks.map { it.title })
    }

    @Test
    fun `undated tasks fall back to priority then notebook then title`() {
        val tasks = TaskGrouping.group(
            listOf(
                task(id = 1, title = "zebra", book = "a", priority = "C"),
                task(id = 2, title = "apple", book = "b", priority = "A"),
                task(id = 3, title = "mango", book = "a", priority = "C"),
                task(id = 4, title = "berry", book = "a", priority = null),
            ),
            now,
            zone,
        ).single().tasks

        // A before C, and C-priority ties break on title; no priority sorts last.
        assertEquals(listOf("apple", "mango", "zebra", "berry"), tasks.map { it.title })
    }

    @Test
    fun `an empty input produces no sections`() {
        assertEquals(emptyList<TaskSection>(), TaskGrouping.group(emptyList(), now, zone))
    }
}
