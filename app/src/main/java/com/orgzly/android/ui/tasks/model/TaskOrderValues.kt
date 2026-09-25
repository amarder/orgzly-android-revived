package com.orgzly.android.ui.tasks.model

/**
 * The hand-picked order within a day, expressed as a number stored on each note.
 *
 * It lives in the org file, in the note's PROPERTIES drawer, because that is the only place
 * that survives everything: loading a notebook from a repo deletes and re-inserts every note
 * in it, so anything Orgzly kept locally against a note id would be scrambled by the first
 * sync that pulled a notebook edited elsewhere.
 */
object TaskOrderValues {

    /**
     * Uppercase with an underscore, matching org's own convention and Orgzly's CREATED. The
     * ORGZLY prefix keeps it clear of both org's reserved names and anything else in the file.
     */
    const val PROPERTY = "ORGZLY_TASK_ORDER"

    /**
     * Gap left between neighbours. Big enough that dropping a task between two others almost
     * always lands on a free number, which is the point: one drag should rewrite one note, not
     * every note sharing its day.
     */
    const val STRIDE = 1024L

    fun parse(value: String?): Long? = value?.trim()?.toLongOrNull()

    /**
     * The order values to write after [moved] was dropped into [group], which is the day in
     * its new visual order. Only notes whose value has to change are returned.
     *
     * The cheap path needs the rest of the day to already be strictly increasing; when it is
     * not - nothing has ever been dragged here, or a file was hand-edited into a tie - there
     * is no single number that produces the right order, so the day is renumbered instead.
     */
    fun assign(group: List<Task>, moved: Long): Map<Long, Long> {
        val index = group.indexOfFirst { it.noteId == moved }
        if (index < 0) return emptyMap()

        val others = group.filterNot { it.noteId == moved }

        val usable = others.all { it.order != null } &&
                others.zipWithNext().all { (a, b) -> a.order!! < b.order!! }

        if (usable) {
            between(group.getOrNull(index - 1)?.order, group.getOrNull(index + 1)?.order)
                ?.let { return mapOf(moved to it) }
        }

        return renumber(group)
    }

    /** A number strictly between the two, or null when they are already adjacent. */
    private fun between(below: Long?, above: Long?): Long? = when {
        below == null && above == null -> 0L
        below == null -> above!! - STRIDE
        above == null -> below + STRIDE
        // Computed as an offset rather than a sum, which would overflow on wide values.
        above - below >= 2 -> below + (above - below) / 2
        else -> null
    }

    private fun renumber(group: List<Task>): Map<Long, Long> =
        group.mapIndexedNotNull { index, task ->
            val value = (index + 1) * STRIDE
            if (task.order == value) null else task.noteId to value
        }.toMap()
}
