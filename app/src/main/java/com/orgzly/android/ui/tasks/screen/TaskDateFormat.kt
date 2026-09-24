package com.orgzly.android.ui.tasks.screen

import android.content.Context
import com.orgzly.R
import org.joda.time.DateTime
import org.joda.time.Days

/** Short, human date label for a due chip: Today / Tomorrow / Yesterday / "Mon 6 Oct". */
fun formatDueDate(context: Context, millis: Long): String {
    val today = DateTime.now().withTimeAtStartOfDay()
    val due = DateTime(millis).withTimeAtStartOfDay()

    return when (Days.daysBetween(today, due).days) {
        0 -> context.getString(R.string.tasks_date_today)
        1 -> context.getString(R.string.tasks_date_tomorrow)
        -1 -> context.getString(R.string.tasks_date_yesterday)
        else -> {
            val pattern = if (due.year == today.year) "EEE d MMM" else "d MMM yyyy"
            due.toString(pattern)
        }
    }
}
