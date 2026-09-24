package com.orgzly.android.ui.tasks.model

import com.orgzly.android.db.entity.NoteView

/**
 * The single point where a database row becomes a [Task]. Keeping it in one place means the
 * rest of the tasks UI never has to know about NoteView's quirks.
 *
 * Note it reads *TimeTimestamp and never *TimeStartOfDay: those columns are declared Long? on
 * NoteView but the SQL selects a datetime() string, so SQLite coerces them to the year number.
 */
fun NoteView.toTask(doneKeywords: Set<String>): Task {
    val ownTags = note.tags?.tags.orEmpty()

    return Task(
        noteId = note.id,
        bookId = note.position.bookId,
        bookName = bookName,
        title = note.title,
        state = note.state,
        isDone = note.state != null && doneKeywords.contains(note.state),
        priority = note.priority,
        tags = ownTags + getInheritedTagsList(),
        hasContent = note.hasContent(),
        scheduledMillis = scheduledTimeTimestamp,
        scheduledRangeString = scheduledRangeString,
        deadlineMillis = deadlineTimeTimestamp,
        deadlineRangeString = deadlineRangeString,
    )
}
