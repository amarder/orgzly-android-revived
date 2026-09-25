package com.orgzly.android.ui.tasks.model

import androidx.compose.runtime.Immutable
import org.joda.time.DateTime

/**
 * A calendar day, with no time of day.
 *
 * The tasks UI only ever schedules whole days, and Android's DatePickerDialog speaks in
 * year/month/day, so carrying those three around avoids converting to a timestamp and back
 * through a timezone on every hop.
 */
@Immutable
data class TaskDate(val year: Int, val month0: Int, val day: Int) {

    /** Local midnight, which is what the list sorts and formats against. */
    fun toMillis(): Long = DateTime(year, month0 + 1, day, 0, 0).millis

    companion object {
        fun today(): TaskDate = of(DateTime.now())

        fun of(millis: Long): TaskDate = of(DateTime(millis))

        /** [month0] is 0-based, matching DatePickerDialog, Calendar and OrgDateTime.Builder. */
        private fun of(dateTime: DateTime) =
            TaskDate(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth)
    }
}
