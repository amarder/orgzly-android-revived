package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.tasks.model.NotebookSelection
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskGrouping
import com.orgzly.android.ui.tasks.model.toTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.joda.time.DateTime

class TasksViewModel(
    private val dataRepository: DataRepository,
    private val context: Context,
) : CommonViewModel() {

    private val actions = TasksActions(dataRepository, context)

    private val showArchived = MutableStateFlow(false)
    private val detailNoteId = MutableStateFlow<Long?>(null)
    private val detailContent = MutableStateFlow<String?>(null)
    private val detailLoading = MutableStateFlow(false)

    /**
     * Which bucket a task falls in depends on the wall clock, so re-emit at local midnight to
     * move yesterday's "Today" into "Overdue" without the user having to reopen the screen.
     */
    private val dayTick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            val nextMidnight = DateTime.now().withTimeAtStartOfDay().plusDays(1).millis
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val notes = showArchived.flatMapLatest { archived ->
        dataRepository.selectNotesFromQueryFlow(
            if (archived) QUERY_INCLUDING_ARCHIVED else QUERY
        )
    }

    val state = combine(notes, dayTick, showArchived) { noteViews, now, archived ->
        val doneKeywords = AppPreferences.doneKeywordsSet(context)

        TasksState(
            sections = TaskGrouping.group(noteViews.map { it.toTask(doneKeywords) }, now),
            isLoading = false,
            showArchived = archived,
        )
    }.state(TasksState.initial)

    private val notebooks = dataRepository.getBooksLiveData().asFlow().map { books ->
        books.map { NotebookOption(it.book.id, it.book.name) }
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

        val task = tasksState.sections
            .asSequence()
            .flatMap { it.tasks.asSequence() }
            .firstOrNull { it.noteId == noteId }
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

    fun setShowArchived(show: Boolean) {
        showArchived.value = show
    }

    fun selectQuickAddNotebook(bookId: Long) {
        selectedBookId.value = bookId
        TasksPrefs.quickAddBookId(context, bookId)
    }

    // --- Writes -------------------------------------------------------------------------

    fun toggleDone(task: Task) = write { actions.toggleDone(task.noteId) }

    fun create(title: String) = write {
        actions.create(title, quickAdd.value.selected?.id)
    }

    fun rename(task: Task, title: String) = write { actions.rename(task.noteId, title) }

    fun setContent(task: Task, content: String?) = write { actions.setContent(task.noteId, content) }

    fun setArchived(task: Task, archived: Boolean) = write {
        actions.setArchived(task.noteId, archived)
        closeDetail()
    }

    fun delete(task: Task) = write {
        actions.delete(task.bookId, task.noteId)
        closeDetail()
    }

    fun setScheduledDate(task: Task, year: Int, month0: Int, day: Int) = write {
        actions.setScheduled(
            task.noteId,
            TaskDateEdit.rebase(task.scheduledRangeString, year, month0, day)
        )
    }

    fun clearScheduled(task: Task) = write { actions.setScheduled(task.noteId, null) }

    /** Database writes block, so they never run on the main thread. */
    private fun write(block: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        catchAndPostError(block)
    }

    companion object {
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

        const val QUERY_INCLUDING_ARCHIVED = STATES
    }
}
