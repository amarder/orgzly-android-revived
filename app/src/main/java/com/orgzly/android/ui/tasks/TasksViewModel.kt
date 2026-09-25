package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.compose.base.EventFlow
import com.orgzly.android.ui.tasks.model.NotebookSelection
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskDate
import com.orgzly.android.ui.tasks.model.TaskOrderValues
import com.orgzly.android.ui.tasks.model.TaskOrdering
import com.orgzly.android.ui.tasks.model.toTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TasksViewModel(
    private val dataRepository: DataRepository,
    private val database: OrgzlyDatabase,
    private val context: Context,
) : CommonViewModel() {

    private val actions = TasksActions(dataRepository, context)

    /**
     * Rows hidden from the list. Grows when a delete starts and shrinks only on undo -
     * committing does not touch it.
     *
     * Visibility and undo bookkeeping are separate on purpose. When they were one map,
     * clearing the entry on commit changed visibility a moment before the query re-emitted
     * without the note, and the row flashed back for a frame. Because note ids are
     * AUTOINCREMENT they are never reused, so leaving a deleted id here forever is safe.
     */
    private val hiddenIds = MutableStateFlow<Set<Long>>(emptySet())

    /**
     * Deletes wait here until their undo window closes, so the row vanishes at once but the
     * org file is untouched. Recreating a deleted note instead would lose its id and its
     * place in the outline, which is not an undo.
     */
    private val pendingDeletes = MutableStateFlow<Map<Long, Task>>(emptyMap())
    private val detailNoteId = MutableStateFlow<Long?>(null)
    private val detailContent = MutableStateFlow<String?>(null)
    private val detailLoading = MutableStateFlow(false)


    /**
     * Order values written by a drag but not yet seen coming back out of the database.
     *
     * Without this the list would show the old order for the length of one round trip after
     * the finger lifts, which reads as the drag having been refused.
     */
    private val pendingOrders = MutableStateFlow<Map<Long, Long>>(emptyMap())

    private val notes = dataRepository.selectNotesFromQueryFlow(QUERY)

    private val books: Flow<List<BookView>> = dataRepository.getBooksLiveData().asFlow()

    val state = combine(notes, hiddenIds, pendingOrders) { noteViews, hidden, pending ->
        val doneKeywords = AppPreferences.doneKeywordsSet(context)

        val orders = readTaskOrders(database) + pending

        val tasks = noteViews
            .map { it.toTask(doneKeywords, orders) }
            .filterNot { it.noteId in hidden }

        TasksState(tasks = TaskOrdering.sort(tasks), isLoading = false)
    }.flowOn(Dispatchers.IO).state(TasksState.initial)

    /**
     * What the sync button shows. Both halves are needed: the worker knows whether a sync is
     * running or failed, and only the notebooks know whether there is local work waiting.
     *
     * SyncRunner is read directly rather than through SyncProgressViewModel because that one
     * drops nulls, and null is how "has never synced in this process" arrives.
     */
    val syncStatus = combine(
        SyncRunner.onStateChange(SYNC_TAG).asFlow(), books
    ) { syncState, bookViews ->
        syncStatusOf(
            state = syncState,
            hasError = bookViews.any { it.book.lastAction?.type == BookAction.Type.ERROR },
            hasPending = bookViews.any { it.isOutOfSync() },
        )
    }.state(SyncStatus.SYNCED)

    private val _events = EventFlow<TasksEvent>()
    val events = _events.asFlow(viewModelScope)

    private val notebooks = books.map { bookViews ->
        bookViews.map { NotebookOption(it.book.id, it.book.name) }
    }

    private val selectedBookId = MutableStateFlow(TasksPrefs.quickAddBookId(context))

    val quickAdd = combine(notebooks, selectedBookId) { books, selected ->
        QuickAddState(
            notebooks = books,
            selected = NotebookSelection.resolve(
                notebooks = books,
                remembered = selected,
                captureName = AppPreferences.shareNotebook(context),
                idOf = NotebookOption::id,
                nameOf = NotebookOption::name,
            ),
        )
    }.state(QuickAddState.initial)

    val detail = combine(
        detailNoteId, state, detailContent, detailLoading
    ) { noteId, tasksState, content, loading ->
        if (noteId == null) return@combine null

        val task = tasksState.tasks.firstOrNull { it.noteId == noteId }
            ?: return@combine null

        TaskDetailState(task = task, content = content, isLoadingContent = loading)
    }.state(null)

    // --- UI state -----------------------------------------------------------------------

    fun openDetail(noteId: Long) {
        detailNoteId.value = noteId
        detailContent.value = null
        detailLoading.value = true

        write { detailContent.value = actions.contentOf(noteId) }
            .invokeOnCompletion { detailLoading.value = false }
    }

    fun closeDetail() {
        detailNoteId.value = null
        detailContent.value = null
        detailLoading.value = false
    }

    fun selectQuickAddNotebook(bookId: Long) {
        selectedBookId.value = bookId
        TasksPrefs.quickAddBookId(context, bookId)
    }

    // --- Writes -------------------------------------------------------------------------

    fun toggleDone(task: Task) = write { actions.toggleDone(task.noteId) }

    /** One task per line, from the paste sheet. */
    fun createMany(titles: List<String>) = write {
        val created = actions.createMany(titles, quickAdd.value.selected?.id)
        if (created > 0) _events.send(TasksEvent.Added(created))
    }

    /**
     * Records where [moved] ended up, given [group] - its day, in the order just dragged into.
     *
     * Usually one note is written: the order is a number on each note with wide gaps between
     * neighbours, so a drop normally lands on a free value. The whole day is renumbered only
     * when it has to be, because each write is a real edit to the org file.
     */
    fun reorder(moved: Long, group: List<Task>) = write {
        val changes = TaskOrderValues.assign(group, moved)
        if (changes.isEmpty()) return@write

        pendingOrders.value = pendingOrders.value + changes

        changes.forEach { (noteId, value) -> actions.setOrder(noteId, value) }

        // Safe to drop now rather than waiting for the query: the values are committed, so
        // whenever the notes flow next emits it will already be carrying them.
        pendingOrders.value = pendingOrders.value - changes.keys
    }

    fun rename(task: Task, title: String) = write { actions.rename(task.noteId, title) }

    fun setContent(task: Task, content: String?) = write { actions.setContent(task.noteId, content) }

    fun setArchived(task: Task, archived: Boolean) = write {
        actions.setArchived(task.noteId, archived)
        closeDetail()
        if (archived) _events.send(TasksEvent.Archived(task))
    }

    /** Hides the row and starts the undo window; nothing is written yet. */
    fun requestDelete(task: Task) {
        hiddenIds.value = hiddenIds.value + task.noteId
        pendingDeletes.value = pendingDeletes.value + (task.noteId to task)
        closeDetail()
        viewModelScope.launch { _events.send(TasksEvent.Deleted(task)) }
    }

    fun undoDelete(task: Task) {
        pendingDeletes.value = pendingDeletes.value - task.noteId
        hiddenIds.value = hiddenIds.value - task.noteId
    }

    fun commitDelete(task: Task) = write {
        if (pendingDeletes.value.containsKey(task.noteId)) {
            pendingDeletes.value = pendingDeletes.value - task.noteId
            // hiddenIds is intentionally untouched: the row is already hidden and must stay
            // hidden until the query catches up, otherwise it flashes back.
            actions.delete(task.bookId, task.noteId)
        }
    }

    /**
     * Flushes anything still waiting, for when the screen goes away before the undo window
     * closes. Deliberately not on viewModelScope: that is already cancelled by onCleared.
     */
    fun commitPendingDeletes() {
        val pending = pendingDeletes.value
        if (pending.isEmpty()) return

        pendingDeletes.value = emptyMap()

        App.EXECUTORS.diskIO().execute {
            pending.values.forEach { task ->
                try {
                    actions.delete(task.bookId, task.noteId)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCleared() {
        commitPendingDeletes()
        super.onCleared()
    }

    fun setScheduledDate(task: Task, year: Int, month0: Int, day: Int) = write {
        actions.setScheduled(
            task.noteId,
            TaskDateEdit.rebase(task.scheduledRangeString, year, month0, day)
        )
    }

    fun clearScheduled(task: Task) = write { actions.setScheduled(task.noteId, null) }

    /**
     * Whether the new-task screen is up. Kept here rather than in the composition so it
     * survives a rotation mid-thought.
     */
    private val composingNewTask = MutableStateFlow(false)

    val isComposingNewTask = composingNewTask.asStateFlow()

    fun startNewTask() {
        composingNewTask.value = true
    }

    fun cancelNewTask() {
        composingNewTask.value = false
    }

    /**
     * Creates the task and closes the screen at once, without waiting for the write.
     *
     * The list is driven by the database, so the row appears when it appears; holding the
     * screen open until then would only make a fast thing feel slow.
     */
    fun createTask(title: String, content: String?, date: TaskDate?) {
        composingNewTask.value = false

        write { actions.create(title, content, quickAdd.value.selected?.id, date) }
    }

    /** Database writes block, so they never run on the main thread. */
    private fun write(block: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
            errorEvent.postValue(e)
        }
    }

    companion object {
        /** Identifies this screen in SyncRunner's state logging. */
        private const val SYNC_TAG = "tasks"

        private const val STATES = "(it.todo or it.done)"

        /**
         * Every note in a to-do *or* done state, across all notebooks. Keywords for both come
         * from the user's own workflow settings; see SqliteQueryBuilder's handling of
         * StateType. Plain headings have no state and so are excluded either way.
         *
         * Done notes are included so that checking a task off leaves it in place, struck
         * through, instead of making it vanish. Note this is unbounded: every note ever
         * completed is loaded, which on a large set of org files is a lot of rows.
         *
         * ".t.ARCHIVE" excludes notes tagged ARCHIVE and, because the condition also matches
         * inherited tags, everything filed underneath an archived note.
         *
         * Deliberately unsorted: the effective due date is min(scheduled, deadline), which the
         * SQL builder cannot express, so ordering happens in Kotlin alongside grouping.
         */
        const val QUERY = "$STATES .t.${Task.ARCHIVE_TAG}"
    }
}
