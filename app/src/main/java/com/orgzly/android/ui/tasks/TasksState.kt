package com.orgzly.android.ui.tasks

import androidx.compose.runtime.Immutable
import com.orgzly.android.ui.tasks.model.Task

@Immutable
data class TasksState(
    val tasks: List<Task>,
    val isLoading: Boolean,
) {
    val isEmpty: Boolean get() = tasks.isEmpty()

    companion object {
        val initial = TasksState(tasks = emptyList(), isLoading = true)
    }
}

/** The task currently open in the detail sheet, plus its lazily loaded body text. */
@Immutable
data class TaskDetailState(
    val task: Task,
    val content: String?,
    val isLoadingContent: Boolean,
)

@Immutable
data class NotebookOption(val id: Long, val name: String)

/** Notebook choice for the quick-add sheet. */
@Immutable
data class QuickAddState(
    val notebooks: List<NotebookOption>,
    val selected: NotebookOption?,
) {
    companion object {
        val initial = QuickAddState(notebooks = emptyList(), selected = null)
    }
}
