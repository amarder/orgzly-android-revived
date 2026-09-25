package com.orgzly.android.ui.tasks.model

/**
 * One row of the rendered task list: either a date divider, or a task carrying [taskIndex] -
 * its position in the sorted task list. [DraggableTaskList][com.orgzly.android.ui.tasks.screen
 * .DraggableTaskList]'s drag math needs that position directly once header rows are interleaved
 * into the same LazyColumn, at which point the LazyColumn's own item index no longer matches it.
 */
sealed interface TaskListEntry {
    data class Header(val dayBucket: Long?) : TaskListEntry
    data class Row(val task: Task, val taskIndex: Int) : TaskListEntry
}

/**
 * Builds the header+row stream the task list renders. Pure, so it can be tested without
 * Android or Robolectric, same as [TaskOrdering].
 *
 * Relies on [tasks] already being grouped contiguously by day - guaranteed by
 * [TaskOrdering.sort]'s comparator, whose first key is [TaskOrdering.dayBucket].
 */
object TaskListEntries {
    fun build(tasks: List<Task>): List<TaskListEntry> {
        val entries = mutableListOf<TaskListEntry>()
        var previousBucket: Long? = null

        tasks.forEachIndexed { index, task ->
            val bucket = TaskOrdering.dayBucket(task.dueMillis)
            if (index == 0 || bucket != previousBucket) {
                entries += TaskListEntry.Header(bucket)
            }
            entries += TaskListEntry.Row(task, index)
            previousBucket = bucket
        }

        return entries
    }
}
