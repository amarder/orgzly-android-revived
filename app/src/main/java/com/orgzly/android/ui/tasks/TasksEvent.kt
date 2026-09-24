package com.orgzly.android.ui.tasks

import com.orgzly.android.ui.tasks.model.Task

/** Something the user just did that they may want to take back. */
sealed interface TasksEvent {
    data class Archived(val task: Task) : TasksEvent

    data class Deleted(val task: Task) : TasksEvent
}
