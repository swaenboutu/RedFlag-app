package app.redflag.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Échelle d'un graphe : une barre par jour, par mois ou par année. */
enum class StatsRange(val bucketCount: Int) {
    DAY(14),
    MONTH(12),
    YEAR(5),
}

/**
 * Ce qui s'est passé sur une période commençant le [start] :
 * [blocked] = « Non, fermer l'app », [bypassed] = « Oui, ouvrir », [snoozed] = « Ne plus demander pendant… ».
 */
data class StatsBucket(val start: LocalDate, val blocked: Int, val bypassed: Int, val snoozed: Int) {
    /** Tentatives d'ouverture : chaque interruption affichée en compte une, quelle que soit la réponse. */
    val attempts: Int get() = blocked + bypassed + snoozed
}

object Stats {
    /** Ce qui compte comme réponse : « Non » = fermée, pause = mise en pause, sinon « Oui » = outrepassée. */
    private fun ChoiceEvent.kind() = when {
        !proceeded -> 0
        snoozed -> 2
        else -> 1
    }

    /**
     * Les [StatsRange.bucketCount] dernières périodes de [range], la plus récente à droite (la période en cours), zéros compris.
     * Une période commence le jour même (jour), le 1er du mois (mois) ou le 1er janvier (année).
     */
    fun buckets(
        events: List<ChoiceEvent>,
        range: StatsRange,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<StatsBucket> = buckets(ChoiceHistory(events), range, now, zone)

    /**
     * Same, from the whole history: the answers already counted per month ([ChoiceHistory.months]) go to their month or year. They
     * are older than [HistoryCompaction.KEEP_MILLIS], so never in the per-day view.
     */
    fun buckets(
        history: ChoiceHistory,
        range: StatsRange,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<StatsBucket> {
        val events = history.events
        val today = now.atZone(zone).toLocalDate()
        val starts = (range.bucketCount - 1 downTo 0).map { back ->
            when (range) {
                StatsRange.DAY -> today.minusDays(back.toLong())
                StatsRange.MONTH -> today.withDayOfMonth(1).minusMonths(back.toLong())
                StatsRange.YEAR -> today.withDayOfYear(1).minusYears(back.toLong())
            }
        }
        val counts = starts.associateWith { IntArray(3) }
        events.forEach { event ->
            val day = Instant.ofEpochMilli(event.timestamp).atZone(zone).toLocalDate()
            val start = when (range) {
                StatsRange.DAY -> day
                StatsRange.MONTH -> day.withDayOfMonth(1)
                StatsRange.YEAR -> day.withDayOfYear(1)
            }
            counts[start]?.let { it[event.kind()]++ }
        }
        if (range != StatsRange.DAY) {
            history.months.forEach { month ->
                val first = YearMonth.parse(month.month).atDay(1)
                val start = if (range == StatsRange.MONTH) first else first.withDayOfYear(1)
                counts[start]?.let {
                    it[0] += month.blocked
                    it[1] += month.bypassed
                    it[2] += month.snoozed
                }
            }
        }
        return starts.map { start ->
            val c = counts.getValue(start)
            StatsBucket(start, blocked = c[0], bypassed = c[1], snoozed = c[2])
        }
    }
}
