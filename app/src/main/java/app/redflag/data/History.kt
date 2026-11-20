package app.redflag.data

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Every interruption shown during that month, whatever the answer. */
val ChoiceMonth.attempts: Int get() = blocked + bypassed + snoozed

/**
 * Every answer to the interruption screen: the recent ones one by one ([events]), the older ones counted per app and per month
 * ([months], see [HistoryCompaction]). The two never overlap: an answer is either still in [events] or already counted in [months].
 * Totals, statistics and "launches avoided" add both, so compacting the history never loses a count.
 */
data class ChoiceHistory(val events: List<ChoiceEvent> = emptyList(), val months: List<ChoiceMonth> = emptyList()) {
    /** Only the answers for [packageName]. */
    fun forApp(packageName: String) =
        ChoiceHistory(events.filter { it.packageName == packageName }, months.filter { it.packageName == packageName })

    /** Every interruption shown, whatever the answer. */
    val attempts: Int get() = events.size + months.sumOf { it.attempts }

    /** Interruptions shown per app, whatever the answer. */
    fun attemptsByApp(): Map<String, Int> = sumByApp({ 1 }, { it.attempts })

    /** "No" answers per app. */
    fun refusalsByApp(): Map<String, Int> = sumByApp({ if (it.proceeded) 0 else 1 }, { it.blocked })

    private fun sumByApp(event: (ChoiceEvent) -> Int, month: (ChoiceMonth) -> Int): Map<String, Int> {
        val totals = HashMap<String, Int>()
        events.forEach { totals.merge(it.packageName, event(it), Int::plus) }
        months.forEach { totals.merge(it.packageName, month(it), Int::plus) }
        return totals.filterValues { it > 0 }
    }
}

/**
 * Keeps the history small whatever how long the app is used: the answers older than [KEEP_MILLIS] are added to their month's
 * counts ([ChoiceMonth]), then removed. The views that need single answers (per day over 14 days, rules over 3 months) only look
 * at recent ones; the per-month and per-year views and the totals read the counts.
 */
object HistoryCompaction {
    /** Answers stay one by one this long: more than the per-day view (14 days) and the rules over 3 months. */
    const val KEEP_MILLIS = 120 * 24 * 60 * 60_000L

    /** Compacting is not worth doing more often than this. */
    const val EVERY_MILLIS = 24 * 60 * 60_000L

    /** The month of [timestamp] in [zone], as stored in [ChoiceMonth.month] ("2026-09"). */
    fun monthOf(timestamp: Long, zone: ZoneId): String = YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(zone)).toString()

    /** [events] counted per app and per month. */
    fun summarize(events: List<ChoiceEvent>, zone: ZoneId): List<ChoiceMonth> =
        events
            .groupBy { it.packageName to monthOf(it.timestamp, zone) }
            .map { (key, list) ->
                ChoiceMonth(
                    packageName = key.first,
                    month = key.second,
                    blocked = list.count { !it.proceeded },
                    bypassed = list.count { it.proceeded && !it.snoozed },
                    snoozed = list.count { it.proceeded && it.snoozed },
                )
            }

    /** [existing] counts with [added] ones: the same app and month are added up, the others kept as they are. */
    fun merge(existing: List<ChoiceMonth>, added: List<ChoiceMonth>): List<ChoiceMonth> =
        (existing + added)
            .groupBy { it.packageName to it.month }
            .map { (key, list) ->
                ChoiceMonth(key.first, key.second, list.sumOf { it.blocked }, list.sumOf { it.bypassed }, list.sumOf { it.snoozed })
            }

    /** What the history becomes once compacted at [now]: the same totals, with only the recent answers kept one by one. */
    fun compact(history: ChoiceHistory, now: Long, zone: ZoneId): ChoiceHistory {
        val cutoff = now - KEEP_MILLIS
        val (old, recent) = history.events.partition { it.timestamp < cutoff }
        return ChoiceHistory(recent, merge(history.months, summarize(old, zone)))
    }
}
