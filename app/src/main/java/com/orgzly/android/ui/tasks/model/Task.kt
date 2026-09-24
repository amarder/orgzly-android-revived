package com.orgzly.android.ui.tasks.model

import androidx.compose.runtime.Immutable

enum class DueKind { SCHEDULED, DEADLINE }

@Immutable
data class TaskDue(val millis: Long, val kind: DueKind)

/**
 * Flattened, UI-facing view of a single to-do note. Deliberately holds no [com.orgzly.android
 * .db.entity.NoteView] so that grouping and sorting stay free of Android and database types.
 */
@Immutable
data class Task(
    val noteId: Long,
    val bookId: Long,
    val bookName: String,
    val title: String,
    val state: String?,
    val isDone: Boolean,
    val priority: String?,
    val tags: List<String>,
    val hasContent: Boolean,
    val scheduledMillis: Long?,
    val scheduledRangeString: String?,
    val deadlineMillis: Long?,
    val deadlineRangeString: String?,
) {
    val isArchived: Boolean
        get() = tags.any { it.equals(ARCHIVE_TAG, ignoreCase = true) }

    /**
     * The date this task is answerable to: whichever of SCHEDULED/DEADLINE lands first.
     * A deadline wins a tie, because it is the harder commitment of the two.
     */
    val due: TaskDue?
        get() {
            val s = scheduledMillis
            val d = deadlineMillis
            return when {
                s != null && d != null ->
                    if (d <= s) TaskDue(d, DueKind.DEADLINE) else TaskDue(s, DueKind.SCHEDULED)
                d != null -> TaskDue(d, DueKind.DEADLINE)
                s != null -> TaskDue(s, DueKind.SCHEDULED)
                else -> null
            }
        }

    companion object {
        /** Orgzly's existing convention; see NoteItemViewBinder.ARCHIVE_TAG. */
        const val ARCHIVE_TAG = "ARCHIVE"
    }
}
