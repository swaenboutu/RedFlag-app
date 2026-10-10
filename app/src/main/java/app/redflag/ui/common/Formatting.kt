package app.redflag.ui.common

import android.content.Context
import android.text.format.DateUtils
import app.redflag.R

/** « 15 minutes », « 1 heure », « 24 heures » : dans la langue courante, avec le bon pluriel. */
fun formatPause(context: Context, minutes: Int): String =
    if (minutes % 60 == 0) {
        val hours = minutes / 60
        context.resources.getQuantityString(R.plurals.duration_hours, hours, hours)
    } else {
        context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
    }

/** "3:30 PM" when [millis] is today, otherwise the date as well: when a pause ends. */
fun formatResumeTime(context: Context, millis: Long): String {
    val sameDay = DateUtils.isToday(millis)
    val flags = DateUtils.FORMAT_SHOW_TIME or if (sameDay) 0 else DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
    return DateUtils.formatDateTime(context, millis, flags)
}
