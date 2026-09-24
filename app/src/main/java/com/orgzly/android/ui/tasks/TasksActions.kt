package com.orgzly.android.ui.tasks

import android.content.Context
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.note.NoteBuilder
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.usecase.NoteCreate
import com.orgzly.android.usecase.NoteDelete
import com.orgzly.android.usecase.NoteUpdate
import com.orgzly.android.usecase.NoteUpdateDeadlineTime
import com.orgzly.android.usecase.NoteUpdateScheduledTime
import com.orgzly.android.usecase.NoteUpdateStateToggle
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import java.util.Calendar

/**
 * Every write the tasks UI performs. All of them go through UseCaseRunner rather than touching
 * DataRepository directly, because that is what marks the notebook modified and triggers
 * auto-sync, reminder rescheduling and widget refresh.
 *
 * These calls block on the database; callers are responsible for being off the main thread.
 */
class TasksActions(
    private val dataRepository: DataRepository,
    private val context: Context,
) {

    fun toggleDone(noteId: Long) {
        // Handles the CLOSED timestamp, repeater shifting and LOGBOOK entries for us.
        UseCaseRunner.run(NoteUpdateStateToggle(setOf(noteId)))
    }

    fun delete(bookId: Long, noteId: Long) {
        UseCaseRunner.run(NoteDelete(bookId, setOf(noteId)))
    }

    /** Archiving is the ARCHIVE tag, which is the convention Orgzly already dims items on. */
    fun setArchived(noteId: Long, archived: Boolean) {
        val payload = dataRepository.getNotePayload(noteId) ?: return

        val withoutTag = payload.tags.filterNot { it.equals(Task.ARCHIVE_TAG, ignoreCase = true) }
        val tags = if (archived) withoutTag + Task.ARCHIVE_TAG else withoutTag

        if (tags == payload.tags) return

        UseCaseRunner.run(NoteUpdate(noteId, payload.copy(tags = tags)))
    }

    fun rename(noteId: Long, title: String) {
        val payload = dataRepository.getNotePayload(noteId) ?: return
        val trimmed = title.trim()

        if (trimmed.isEmpty() || trimmed == payload.title) return

        UseCaseRunner.run(NoteUpdate(noteId, payload.copy(title = trimmed)))
    }

    fun setContent(noteId: Long, content: String?) {
        val payload = dataRepository.getNotePayload(noteId) ?: return
        val normalised = content?.takeIf { it.isNotBlank() }

        if (normalised == payload.content) return

        UseCaseRunner.run(NoteUpdate(noteId, payload.copy(content = normalised)))
    }

    fun setScheduled(noteId: Long, time: OrgDateTime?) {
        UseCaseRunner.run(NoteUpdateScheduledTime(setOf(noteId), time))
    }

    fun setDeadline(noteId: Long, time: OrgDateTime?) {
        UseCaseRunner.run(NoteUpdateDeadlineTime(setOf(noteId), time))
    }

    /**
     * Creates a note, forcing a to-do state and a SCHEDULED date of today.
     *
     * Both are overridden rather than left to NoteBuilder. It takes the state from the "new
     * note state" preference, which may be blank or "NOTE" - either would produce a task the
     * it.todo query cannot see - and it schedules only when "new note scheduled" happens to
     * be on, which would drop new tasks into "No date" where they are easy to lose.
     *
     * A null [bookId] falls back to Orgzly's own capture target, which creates the default
     * notebook when none exist yet.
     */
    fun create(title: String, bookId: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return

        val targetBookId = bookId ?: dataRepository.getTargetBook(context).book.id

        UseCaseRunner.run(NoteCreate(newTaskPayload(trimmed), NotePlace(targetBookId)))
    }

    /** Split out from [create] so it can be tested without going through Dagger. */
    internal fun newTaskPayload(title: String): NotePayload {
        val today = Calendar.getInstance()

        val scheduled = OrgRange(
            TaskDateEdit.rebase(
                existingRangeString = null,
                year = today.get(Calendar.YEAR),
                month0 = today.get(Calendar.MONTH),
                day = today.get(Calendar.DAY_OF_MONTH),
            )
        ).toString()

        return NoteBuilder.newPayload(context, title, null).copy(
            state = AppPreferences.getFirstTodoState(context),
            scheduled = scheduled,
        )
    }

    fun contentOf(noteId: Long): String? = dataRepository.getNotePayload(noteId)?.content
}
