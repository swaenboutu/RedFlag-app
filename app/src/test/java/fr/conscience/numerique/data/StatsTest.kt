package fr.conscience.numerique.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val now = LocalDateTime.of(2026, 9, 30, 12, 0).atZone(zone).toInstant()

    private fun event(year: Int, month: Int, day: Int, proceeded: Boolean, snoozed: Boolean = false) = ChoiceEvent(
        packageName = "app",
        timestamp = LocalDateTime.of(year, month, day, 9, 30).atZone(zone).toInstant().toEpochMilli(),
        proceeded = proceeded,
        snoozed = snoozed,
    )

    @Test
    fun `sans evenement, toutes les barres existent et valent zero`() {
        val buckets = Stats.buckets(emptyList(), StatsRange.DAY, now, zone)
        assertEquals(14, buckets.size)
        assertEquals(LocalDate.of(2026, 9, 30), buckets.last().start)
        assertEquals(LocalDate.of(2026, 9, 17), buckets.first().start)
        assertEquals(0, buckets.sumOf { it.attempts })
    }

    @Test
    fun `les reponses sont classees en fermee, outrepassee ou mise en pause`() {
        val events = listOf(
            event(2026, 9, 30, proceeded = false),
            event(2026, 9, 30, proceeded = true),
            event(2026, 9, 30, proceeded = true),
            event(2026, 9, 30, proceeded = true, snoozed = true),
        )
        val today = Stats.buckets(events, StatsRange.DAY, now, zone).last()
        assertEquals(1, today.blocked)
        assertEquals(2, today.bypassed)
        assertEquals(1, today.snoozed)
        assertEquals(4, today.attempts)
    }

    @Test
    fun `par jour, un evenement plus ancien que la fenetre est ignore`() {
        val buckets = Stats.buckets(listOf(event(2026, 9, 16, proceeded = false)), StatsRange.DAY, now, zone)
        assertEquals(0, buckets.sumOf { it.attempts })
        val kept = Stats.buckets(listOf(event(2026, 9, 17, proceeded = false)), StatsRange.DAY, now, zone)
        assertEquals(1, kept.first().blocked)
    }

    @Test
    fun `par mois, les evenements d'un meme mois sont regroupes`() {
        val events = listOf(
            event(2026, 9, 1, proceeded = false),
            event(2026, 9, 30, proceeded = true),
            event(2026, 8, 31, proceeded = true),
            event(2025, 9, 15, proceeded = true),
        )
        val buckets = Stats.buckets(events, StatsRange.MONTH, now, zone)
        assertEquals(12, buckets.size)
        assertEquals(LocalDate.of(2025, 10, 1), buckets.first().start)
        assertEquals(LocalDate.of(2026, 9, 1), buckets.last().start)
        assertEquals(2, buckets.last().attempts)
        assertEquals(1, buckets[buckets.size - 2].attempts)
        // Septembre 2025 est hors de la fenêtre de 12 mois.
        assertEquals(3, buckets.sumOf { it.attempts })
    }

    @Test
    fun `par annee, les cinq dernieres annees`() {
        val events = listOf(
            event(2026, 1, 2, proceeded = true),
            event(2024, 12, 31, proceeded = false),
            event(2021, 6, 1, proceeded = true),
        )
        val buckets = Stats.buckets(events, StatsRange.YEAR, now, zone)
        assertEquals(listOf(2022, 2023, 2024, 2025, 2026), buckets.map { it.start.year })
        assertEquals(1, buckets.last().bypassed)
        assertEquals(1, buckets[2].blocked)
        assertEquals(2, buckets.sumOf { it.attempts })
    }
}
