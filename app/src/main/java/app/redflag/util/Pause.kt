package app.redflag.util

import java.util.Calendar
import java.util.TimeZone

/** How long the interruption is paused from the home screen. */
sealed interface PauseChoice {
    /** For [minutes] minutes. */
    data class Minutes(val minutes: Int) : PauseChoice

    /** Until the next morning, at [MORNING_HOUR] o'clock. */
    data object UntilMorning : PauseChoice

    /** Until the user turns the interruption back on. */
    data object UntilTurnedBackOn : PauseChoice

    companion object {
        /** The choices offered, in the order of the pause sheet. */
        val ALL: List<PauseChoice> = listOf(Minutes(15), Minutes(60), Minutes(4 * 60), UntilMorning, UntilTurnedBackOn)

        /** The one selected when the sheet opens. */
        val DEFAULT: PauseChoice = Minutes(60)
    }
}

const val MORNING_HOUR = 8

/** When the pause of [choice] ends, started at [now] (epoch milliseconds); null = it does not end by itself. */
fun PauseChoice.resumeAt(now: Long, zone: TimeZone = TimeZone.getDefault()): Long? = when (this) {
    is PauseChoice.Minutes -> now + minutes * 60_000L
    PauseChoice.UntilMorning -> nextMorning(now, zone)
    PauseChoice.UntilTurnedBackOn -> null
}

/** The next [MORNING_HOUR]:00 after [now]: today's if it is earlier than that, otherwise tomorrow's. */
fun nextMorning(now: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, MORNING_HOUR)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_MONTH, 1)
    return calendar.timeInMillis
}
