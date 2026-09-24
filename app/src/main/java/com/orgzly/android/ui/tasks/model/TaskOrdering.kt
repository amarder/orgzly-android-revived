package com.orgzly.android.ui.tasks.model

/**
 * Order of the task list. Pure, so it can be tested without Android or Robolectric.
 *
 * The list is flat and unheaded, so this ordering is the only thing telling the user what is
 * urgent: soonest first, undated last. State is deliberately absent, which is what lets a
 * task stay exactly where it is when it is checked off.
 */
object TaskOrdering {

    fun sort(tasks: List<Task>): List<Task> = tasks.sortedWith(comparator)

    /**
     * Undated tasks all compare equal on the due date, so the remaining keys decide their
     * order. Absent priority sorts last rather than first, which is why null maps to "￿".
     */
    private val comparator: Comparator<Task> =
        compareBy<Task> { it.dueMillis ?: Long.MAX_VALUE }
            .thenBy { it.priority?.uppercase() ?: "￿" }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.bookName }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
}
