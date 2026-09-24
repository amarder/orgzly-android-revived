package com.orgzly.android.ui.tasks.model

import androidx.annotation.StringRes
import com.orgzly.R

/** Date buckets the task list is grouped into, in display order. */
enum class TaskBucket(@param:StringRes val titleRes: Int) {
    OVERDUE(R.string.tasks_bucket_overdue),
    TODAY(R.string.tasks_bucket_today),
    UPCOMING(R.string.tasks_bucket_upcoming),
    LATER(R.string.tasks_bucket_later),
    NO_DATE(R.string.tasks_bucket_no_date),
}

@androidx.compose.runtime.Immutable
data class TaskSection(val bucket: TaskBucket, val tasks: List<Task>)
