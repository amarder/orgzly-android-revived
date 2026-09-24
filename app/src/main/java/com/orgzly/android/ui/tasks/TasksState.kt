package com.orgzly.android.ui.tasks

import androidx.compose.runtime.Immutable
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskSection

@Immutable
data class TasksState(
    val sections: List<TaskSection>,
    val isLoading: Boolean,
    val showArchived: Boolean,
) {
    val isEmpty: Boolean get() = sections.isEmpty()

    companion object {
        val initial = TasksState(
            sections = emptyList(),
            isLoading = true,
            showArchived = false,
        )
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
