package com.orgzly.android.ui.tasks.model

import org.joda.time.DateTime

/**
 * Order of the task list. Pure, so it can be tested without Android or Robolectric.
 *
 * The list is flat and unheaded, so this ordering is the only thing telling the user what is
 * urgent: soonest first, undated last. State is deliberately absent, which is what lets a
 * task stay exactly where it is when it is checked off.
 *
 * Within a single day the user can override all of it by dragging, which is what [Task.order]
 * carries - a number stored on the note itself, so it travels with the org file.
 */
object TaskOrdering {

    /**
     * The run of rows around [index] sharing its due day.
     *
     * A drag is clamped to this, which is what stops a task being reordered onto a different
     * date. The list is sorted by day, so the run is contiguous and scanning outwards finds
     * all of it.
     */
    fun dayRange(tasks: List<Task>, index: Int): IntRange {
        val day = dayBucket(tasks[index].dueMillis)

        var first = index
        while (first > 0 && dayBucket(tasks[first - 1].dueMillis) == day) {
            first--
        }

        var last = index
        while (last < tasks.lastIndex && dayBucket(tasks[last + 1].dueMillis) == day) {
            last++
        }

        return first..last
    }

    /** Local midnight of the day a task is due; null for undated. */
    fun dayBucket(millis: Long?): Long? =
        millis?.let { DateTime(it).withTimeAtStartOfDay().millis }

    fun sort(tasks: List<Task>): List<Task> = tasks.sortedWith(comparator)

    /**
     * Undated tasks all compare equal on the due date, so the remaining keys decide their
     * order. Absent priority sorts last rather than first, which is why null maps to "￿".
     *
     * The first key is the *day*, not the timestamp: a hand-placed order has to be able to
     * lift a task with a clock time above an untimed one due the same day, which it could
     * not do if the raw timestamp outranked it.
     */
    private val comparator: Comparator<Task> =
        compareBy<Task> { dayBucket(it.dueMillis) ?: Long.MAX_VALUE }
            .thenBy { it.order ?: Long.MAX_VALUE }
            .thenBy { it.dueMillis ?: Long.MAX_VALUE }
            .thenBy { it.priority?.uppercase() ?: "￿" }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.bookName }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
}
