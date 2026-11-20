package app.redflag.data

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCompactionTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val now = LocalDateTime.of(2026, 11, 20, 12, 0).atZone(zone).toInstant()
    private val nowMillis = now.toEpochMilli()
    private val day = 24 * 60 * 60_000L

    /** [count] random answers for a few apps over the last [days] days. */
    private fun randomHistory(seed: Int, count: Int = 2_000, days: Int = 3 * 365): List<ChoiceEvent> {
        val random = Random(seed)
        val apps = listOf("app.a", "app.b", "app.c", "app.d")
        return List(count) {
            val proceeded = random.nextBoolean()
            ChoiceEvent(
                packageName = apps.random(random),
                timestamp = nowMillis - random.nextLong(days * day),
                proceeded = proceeded,
                snoozed = proceeded && random.nextInt(4) == 0,
            )
        }
    }

    private fun event(daysAgo: Long, proceeded: Boolean = false, snoozed: Boolean = false, pkg: String = "app.a") =
        ChoiceEvent(packageName = pkg, timestamp = nowMillis - daysAgo * day, proceeded = proceeded, snoozed = snoozed)

    @Test
    fun `compacting changes no statistic, for any range, app or total`() {
        repeat(5) { seed ->
            val before = ChoiceHistory(randomHistory(seed))
            val after = HistoryCompaction.compact(before, nowMillis, zone)

            assertTrue("seed $seed: answers were compacted", after.events.size < before.events.size)
            StatsRange.entries.forEach { range ->
                assertEquals("seed $seed, $range", Stats.buckets(before, range, now, zone), Stats.buckets(after, range, now, zone))
            }
            assertEquals("seed $seed: attempts per app", before.attemptsByApp(), after.attemptsByApp())
            assertEquals("seed $seed: refusals per app", before.refusalsByApp(), after.refusalsByApp())
            assertEquals("seed $seed: total", before.attempts, after.attempts)
            listOf("app.a", "app.b").forEach { pkg ->
                StatsRange.entries.forEach { range ->
                    assertEquals(
                        "seed $seed, $pkg, $range",
                        Stats.buckets(before.forApp(pkg), range, now, zone),
                        Stats.buckets(after.forApp(pkg), range, now, zone),
                    )
                }
            }
        }
    }

    @Test
    fun `compacting twice, or a day later, still changes nothing`() {
        val before = ChoiceHistory(randomHistory(seed = 42))
        val once = HistoryCompaction.compact(before, nowMillis, zone)
        val twice = HistoryCompaction.compact(once, nowMillis, zone)
        assertEquals(once, twice)

        val later = HistoryCompaction.compact(once, nowMillis + 40 * day, zone)
        assertEquals(before.attemptsByApp(), later.attemptsByApp())
        assertEquals(before.refusalsByApp(), later.refusalsByApp())
    }

    @Test
    fun `recent answers stay one by one, older ones are only counted`() {
        val keepDays = HistoryCompaction.KEEP_MILLIS / day
        val history = ChoiceHistory(listOf(event(1), event(keepDays - 1), event(keepDays + 1), event(400)))

        val after = HistoryCompaction.compact(history, nowMillis, zone)

        assertEquals(2, after.events.size)
        assertTrue(after.events.all { it.timestamp >= nowMillis - HistoryCompaction.KEEP_MILLIS })
        assertEquals(2, after.months.sumOf { it.attempts })
    }

    @Test
    fun `an answer exactly at the limit is kept one by one`() {
        val atLimit = ChoiceEvent(packageName = "app.a", timestamp = nowMillis - HistoryCompaction.KEEP_MILLIS, proceeded = false)
        val after = HistoryCompaction.compact(ChoiceHistory(listOf(atLimit)), nowMillis, zone)
        assertEquals(listOf(atLimit), after.events)
        assertTrue(after.months.isEmpty())
    }

    @Test
    fun `the counts tell refusals, openings and pauses apart, per app and per month`() {
        val events = listOf(
            event(200), event(200), event(200, proceeded = true), event(200, proceeded = true, snoozed = true),
            event(200, pkg = "app.b"),
        )
        val months = HistoryCompaction.summarize(events, zone)
        val a = months.single { it.packageName == "app.a" }
        assertEquals(ChoiceMonth("app.a", HistoryCompaction.monthOf(events[0].timestamp, zone), 2, 1, 1), a)
        assertEquals(1, months.single { it.packageName == "app.b" }.blocked)
    }

    @Test
    fun `new counts add up with the ones already stored for the same month`() {
        val existing = listOf(ChoiceMonth("app.a", "2025-03", 5, 2, 1), ChoiceMonth("app.b", "2025-03", 1, 0, 0))
        val added = listOf(ChoiceMonth("app.a", "2025-03", 1, 1, 1), ChoiceMonth("app.a", "2025-04", 3, 0, 0))

        val merged = HistoryCompaction.merge(existing, added).sortedBy { it.packageName + it.month }

        assertEquals(
            listOf(ChoiceMonth("app.a", "2025-03", 6, 3, 2), ChoiceMonth("app.a", "2025-04", 3, 0, 0), ChoiceMonth("app.b", "2025-03", 1, 0, 0)),
            merged,
        )
    }

    @Test
    fun `a month partly compacted is shown whole in the per-month view`() {
        // Two answers in the same month, on both sides of the limit: one is counted, the other stays one by one.
        val keepDays = HistoryCompaction.KEEP_MILLIS / day
        val history = ChoiceHistory(listOf(event(keepDays + 1), event(keepDays - 1)))
        val after = HistoryCompaction.compact(history, nowMillis, zone)
        assertEquals(1, after.events.size)
        assertEquals(1, after.months.sumOf { it.attempts })
        assertEquals(Stats.buckets(history, StatsRange.MONTH, now, zone), Stats.buckets(after, StatsRange.MONTH, now, zone))
    }

    @Test
    fun `counts older than five years are kept in the totals even if no chart shows them`() {
        val history = ChoiceHistory(months = listOf(ChoiceMonth("app.a", "2015-06", 4, 1, 0)))
        assertEquals(0, Stats.buckets(history, StatsRange.YEAR, now, zone).sumOf { it.attempts })
        assertEquals(mapOf("app.a" to 5), history.attemptsByApp())
        assertEquals(mapOf("app.a" to 4), history.refusalsByApp())
    }
}
