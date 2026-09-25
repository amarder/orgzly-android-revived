package com.orgzly.android.ui.tasks

import android.content.Context
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.note.NoteBuilder
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskDate
import com.orgzly.android.ui.tasks.model.TaskOrderValues
import com.orgzly.android.usecase.NoteCreate
import com.orgzly.android.usecase.NoteDelete
import com.orgzly.android.usecase.NoteUpdate
import com.orgzly.android.usecase.NoteUpdateDeadlineTime
import com.orgzly.android.usecase.NoteUpdateScheduledTime
import com.orgzly.android.usecase.NoteUpdateStateToggle
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange

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
     * Records where a task sits within its day, as a property on the note itself.
     *
     * Going through NoteUpdate rather than writing the property row directly is what gets it
     * into the org file: the use case marks the notebook modified, and the exporter writes
     * every property back out into the PROPERTIES drawer.
     */
    fun setOrder(noteId: Long, value: Long) {
        val payload = dataRepository.getNotePayload(noteId) ?: return

        val properties = payload.properties.also {
            it.set(TaskOrderValues.PROPERTY, value.toString())
        }

        UseCaseRunner.run(NoteUpdate(noteId, payload.copy(properties = properties)))
    }

    /**
     * Creates a single task from the new-task screen.
     *
     * Everything the screen does not offer is left to [newTaskPayload]'s defaults, which is
     * the point: a task has a title, a notebook, a date and some notes, and nothing else is
     * worth a field on the way in.
     */
    fun create(title: String, content: String?, bookId: Long?, date: TaskDate?): Boolean {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return false

        val targetBookId = bookId ?: dataRepository.getTargetBook(context).book.id

        val payload = newTaskPayload(trimmed, date)
            .copy(content = content?.takeIf { it.isNotBlank() })

        UseCaseRunner.run(NoteCreate(payload, NotePlace(targetBookId)))

        return true
    }

    /**
     * Creates one task per title, returning how many were made.
     *
     * A loop rather than a batch insert: each NoteCreate is its own transaction with a
     * full-book lft shift, which is O(n) book updates, but n here is the length of a list
     * somebody pasted. AutoSync collapses the repeated triggers. If this ever does get slow,
     * DataRepository.pasteNotes is the real bulk primitive.
     *
     * A null [bookId] falls back to Orgzly's own capture target, which creates the default
     * notebook when none exist yet. It is resolved once, before anything is written: falling
     * back per title could create that notebook and then scatter the rest of the batch across
     * whatever it resolved to next.
     */
    fun createMany(titles: List<String>, bookId: Long?): Int {
        val wanted = titles.map { it.trim() }.filter { it.isNotEmpty() }
        if (wanted.isEmpty()) return 0

        val targetBookId = bookId ?: dataRepository.getTargetBook(context).book.id

        wanted.forEach { title ->
            UseCaseRunner.run(NoteCreate(newTaskPayload(title), NotePlace(targetBookId)))
        }

        return wanted.size
    }

    /**
     * What a new task starts as: a to-do state and a SCHEDULED date of today.
     *
     * Both are overridden rather than left to NoteBuilder. It takes the state from the "new
     * note state" preference, which may be blank or "NOTE" - either would produce a task the
     * it.todo query cannot see - and it schedules only when "new note scheduled" happens to
     * be on, which would drop new tasks into "No date" where they are easy to lose.
     *
     * Kept separate from [createMany] so it can be handed to Orgzly's own note editor, and so
     * it can be tested without going through Dagger.
     */
    @JvmOverloads
    fun newTaskPayload(title: String, date: TaskDate? = TaskDate.today()): NotePayload {
        val scheduled = date?.let {
            OrgRange(
                TaskDateEdit.rebase(
                    existingRangeString = null,
                    year = it.year,
                    month0 = it.month0,
                    day = it.day,
                )
            ).toString()
        }

        return NoteBuilder.newPayload(context, title, null).copy(
            state = AppPreferences.getFirstTodoState(context),
            scheduled = scheduled,
        )
    }

    fun contentOf(noteId: Long): String? = dataRepository.getNotePayload(noteId)?.content
}
