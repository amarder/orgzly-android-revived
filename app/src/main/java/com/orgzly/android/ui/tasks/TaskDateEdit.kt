package com.orgzly.android.ui.tasks

import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange

/**
 * Builds the OrgDateTime for a new SCHEDULED/DEADLINE value.
 *
 * Orgzly's setNotesScheduledTime() wraps whatever OrgDateTime it is handed in a fresh OrgRange,
 * so anything not carried across here is lost. That matters most for repeaters: rescheduling
 * "<2026-09-20 Sun +1w>" with a bare timestamp would silently drop the "+1w" and stop the task
 * recurring. Copying the previous timestamp first preserves the repeater, any delay, and the
 * time-of-day, and we override only the date.
 */
object TaskDateEdit {

    /** [month0] is 0-based, matching OrgDateTime.Builder and java.util.Calendar. */
    fun rebase(existingRangeString: String?, year: Int, month0: Int, day: Int): OrgDateTime {
        val previous = existingRangeString?.let { OrgRange.parseOrNull(it) }?.startTime

        val builder =
            if (previous != null) OrgDateTime.Builder(previous) else OrgDateTime.Builder()

        return builder
            .setIsActive(true)
            .setYear(year)
            .setMonth(month0)
            .setDay(day)
            .build()
    }
}
