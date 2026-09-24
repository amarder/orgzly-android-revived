package com.orgzly.android.ui.tasks.model

import androidx.compose.runtime.Immutable

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

    /** The date this task is answerable to: whichever of SCHEDULED/DEADLINE lands first. */
    val dueMillis: Long?
        get() = listOfNotNull(scheduledMillis, deadlineMillis).minOrNull()

    companion object {
        /** Orgzly's existing convention; see NoteItemViewBinder.ARCHIVE_TAG. */
        const val ARCHIVE_TAG = "ARCHIVE"
    }
}
