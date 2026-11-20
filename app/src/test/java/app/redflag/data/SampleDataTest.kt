package app.redflag.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataTest {
    private val now = 1_800_000_000_000L
    private val packages = listOf("app.a", "app.b", "app.c", "app.d", "app.e")

    @Test
    fun `no app, no answer`() {
        assertTrue(SampleData.events(emptyList(), now, Random(1)).isEmpty())
    }

    @Test
    fun `every answer is within the last 3 months, oldest first`() {
        val events = SampleData.events(packages, now, Random(1))
        assertTrue(events.all { it.timestamp in (now - SampleData.WINDOW_MILLIS)..now })
        assertEquals(events.sortedBy { it.timestamp }, events)
    }

    @Test
    fun `each app gets answers, and only the given apps`() {
        val events = SampleData.events(packages, now, Random(2))
        assertEquals(packages.toSet(), events.map { it.packageName }.toSet())
    }

    @Test
    fun `the number of answers follows the habit`() {
        SampleData.Habit.entries.forEach { habit ->
            repeat(20) { seed ->
                val count = SampleData.answers("app", habit, now, Random(seed)).size
                assertTrue("$habit: $count", count in habit.minAttempts..habit.maxAttempts)
            }
        }
    }

    @Test
    fun `a pause is always a way through, never a refusal`() {
        val events = (0 until 20).flatMap { SampleData.events(packages, now, Random(it)) }
        assertTrue(events.none { it.snoozed && !it.proceeded })
        assertTrue("there are pauses", events.any { it.snoozed })
        assertTrue("there are openings", events.any { it.proceeded && !it.snoozed })
        assertTrue("there are refusals", events.any { !it.proceeded })
    }

    @Test
    fun `the first app is always suggested for uninstalling, whatever the random draw`() {
        repeat(50) { seed ->
            val events = SampleData.events(packages, now, Random(seed))
            val suggested = UninstallSuggestions.suggest(events, now).map { it.packageName }
            assertTrue("seed $seed: $suggested", "app.a" in suggested)
        }
    }

    @Test
    fun `the same seed gives the same data`() {
        assertEquals(SampleData.events(packages, now, Random(7)), SampleData.events(packages, now, Random(7)))
    }
}
