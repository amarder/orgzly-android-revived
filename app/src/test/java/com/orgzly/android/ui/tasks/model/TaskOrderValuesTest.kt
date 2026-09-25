package com.orgzly.android.ui.tasks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every value written here becomes a line in somebody's org file, so the question each test
 * asks is the same: was that write necessary?
 */
class TaskOrderValuesTest {

    private fun task(id: Long, order: Long? = null) = Task(
        noteId = id,
        bookId = 1,
        bookName = "book",
        title = "t$id",
        state = "TODO",
        isDone = false,
        priority = null,
        tags = emptyList(),
        hasContent = false,
        scheduledMillis = null,
        scheduledRangeString = null,
        deadlineMillis = null,
        deadlineRangeString = null,
        order = order,
    )

    // --- parsing -----------------------------------------------------------------------

    @Test
    fun `a property value is read as a number`() {
        assertEquals(1024L, TaskOrderValues.parse("1024"))
        assertEquals(-5L, TaskOrderValues.parse(" -5 "))
    }

    /** Hand-edited nonsense sorts as though never dragged, rather than throwing. */
    @Test
    fun `a value that is not a number is ignored`() {
        assertNull(TaskOrderValues.parse("soon"))
        assertNull(TaskOrderValues.parse(""))
        assertNull(TaskOrderValues.parse(null))
    }

    // --- the cheap path ----------------------------------------------------------------

    @Test
    fun `dropping between two spaced neighbours writes one note`() {
        val group = listOf(task(1, 1024), task(2, 3072), task(3, 2048))

        // 2 was dragged down between 1 and 3... no: the group is the new visual order, so
        // task 2 now sits between 1024 and 2048.
        assertEquals(mapOf(2L to 1536L), TaskOrderValues.assign(group, moved = 2))
    }

    @Test
    fun `dropping at the top writes one note, below the current first`() {
        val group = listOf(task(3, 3072), task(1, 1024), task(2, 2048))

        assertEquals(mapOf(3L to 0L), TaskOrderValues.assign(group, moved = 3))
    }

    @Test
    fun `dropping at the bottom writes one note, above the current last`() {
        val group = listOf(task(2, 2048), task(3, 3072), task(1, 1024))

        assertEquals(mapOf(1L to 4096L), TaskOrderValues.assign(group, moved = 1))
    }

    @Test
    fun `a single task gets a starting value`() {
        assertEquals(mapOf(1L to 0L), TaskOrderValues.assign(listOf(task(1)), moved = 1))
    }

    // --- falling back to renumbering ---------------------------------------------------

    /** The first drag in a day: nothing has a value, so there is nothing to slot between. */
    @Test
    fun `an unordered day is numbered from scratch`() {
        val group = listOf(task(3), task(1), task(2))

        assertEquals(
            mapOf(3L to 1024L, 1L to 2048L, 2L to 3072L),
            TaskOrderValues.assign(group, moved = 1),
        )
    }

    @Test
    fun `a day with no gap left is renumbered`() {
        val group = listOf(task(1, 1), task(3, 3), task(2, 2))

        // 1 and 2 are adjacent, so task 3 cannot be given a value between them. Renumbering
        // touches all three, because none of them already holds its new value.
        assertEquals(
            mapOf(1L to 1024L, 3L to 2048L, 2L to 3072L),
            TaskOrderValues.assign(group, moved = 3),
        )
    }

    @Test
    fun `renumbering does not rewrite notes that already hold the right value`() {
        val group = listOf(task(1, 1024), task(2), task(3, 3072))

        val changes = TaskOrderValues.assign(group, moved = 2)

        assertEquals(mapOf(2L to 2048L), changes)
    }

    @Test
    fun `a day whose values are out of step is renumbered rather than trusted`() {
        // Two notes hand-edited to the same value: no single number orders them.
        val group = listOf(task(1, 5), task(2, 5), task(3))

        val changes = TaskOrderValues.assign(group, moved = 3)

        assertEquals(mapOf(1L to 1024L, 2L to 2048L, 3L to 3072L), changes)
    }

    // --- edges -------------------------------------------------------------------------

    @Test
    fun `a task not in its own group writes nothing`() {
        assertEquals(emptyMap<Long, Long>(), TaskOrderValues.assign(listOf(task(1)), moved = 9))
    }

    @Test
    fun `an empty group writes nothing`() {
        assertEquals(emptyMap<Long, Long>(), TaskOrderValues.assign(emptyList(), moved = 1))
    }

    /** Values far apart must not be midpointed by adding them together. */
    @Test
    fun `wide values do not overflow`() {
        val group = listOf(
            task(1, Long.MIN_VALUE / 2),
            task(3),
            task(2, Long.MAX_VALUE / 2),
        )

        val value = TaskOrderValues.assign(group, moved = 3).getValue(3L)

        assertTrue(value > Long.MIN_VALUE / 2 && value < Long.MAX_VALUE / 2)
    }
}
