package com.orgzly.android.ui.tasks

import com.orgzly.android.ui.tasks.model.Task

/** Something the user just did that they may want to take back, or just want told about. */
sealed interface TasksEvent {
    data class Archived(val task: Task) : TasksEvent

    data class Deleted(val task: Task) : TasksEvent

    /** Tasks pasted in bulk. Reported rather than undoable: there is nothing hidden to
     * restore, and unpicking a whole batch is what the notebook view is for. */
    data class Added(val count: Int) : TasksEvent
}
