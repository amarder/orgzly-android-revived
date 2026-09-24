package com.orgzly.android.ui.tasks.model

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * Pure grouping/sorting logic. Takes "now" and the zone as parameters so it can be unit tested
 * without Android or Robolectric, and so a midnight rollover is just a new call.
 */
object TaskGrouping {

    /** Days ahead of today that still count as UPCOMING rather than LATER. */
    const val UPCOMING_DAYS = 7

    fun bucketOf(
        dueMillis: Long?,
        nowMillis: Long,
        zone: DateTimeZone = DateTimeZone.getDefault(),
    ): TaskBucket {
        if (dueMillis == null) return TaskBucket.NO_DATE

        val todayStart = DateTime(nowMillis, zone).withTimeAtStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)
        val upcomingEnd = todayStart.plusDays(UPCOMING_DAYS)

        return when {
            dueMillis < todayStart.millis -> TaskBucket.OVERDUE
            dueMillis < tomorrowStart.millis -> TaskBucket.TODAY
            dueMillis < upcomingEnd.millis -> TaskBucket.UPCOMING
            else -> TaskBucket.LATER
        }
    }

    /**
     * Groups into [TaskSection]s in [TaskBucket] order, dropping empty buckets.
     * Within a bucket: soonest first, then priority, then notebook, then title.
     */
    fun group(
        tasks: List<Task>,
        nowMillis: Long,
        zone: DateTimeZone = DateTimeZone.getDefault(),
    ): List<TaskSection> {
        val byBucket = tasks.groupBy { bucketOf(it.due?.millis, nowMillis, zone) }

        return TaskBucket.entries.mapNotNull { bucket ->
            byBucket[bucket]
                ?.sortedWith(comparator)
                ?.takeIf { it.isNotEmpty() }
                ?.let { TaskSection(bucket, it) }
        }
    }

    /**
     * Undated tasks all compare equal on [Task.due], so the remaining keys decide their order.
     * Absent priority sorts last rather than first, which is why null maps to "￿".
     */
    private val comparator: Comparator<Task> =
        compareBy<Task> { it.due?.millis ?: Long.MAX_VALUE }
            .thenBy { it.priority?.uppercase() ?: "￿" }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.bookName }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
}
