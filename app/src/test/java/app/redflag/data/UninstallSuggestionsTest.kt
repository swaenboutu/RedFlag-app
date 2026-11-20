package app.redflag.data

import app.redflag.data.UninstallRules.Companion.DAY_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UninstallSuggestionsTest {
    private val now = 1_800_000_000_000L

    /** An answer for [pkg], [daysAgo] days before [now] (a fraction of a day keeps several answers on the same day ordered). */
    private fun event(pkg: String, daysAgo: Double, proceeded: Boolean = false, snoozed: Boolean = false) = ChoiceEvent(
        packageName = pkg,
        timestamp = now - (daysAgo * DAY_MILLIS).toLong(),
        proceeded = proceeded,
        snoozed = snoozed,
    )

    /** [count] refusals of [pkg], one per day, the latest one [latestDaysAgo] days ago. */
    private fun refusals(pkg: String, count: Int, latestDaysAgo: Double = 1.0) =
        (0 until count).map { event(pkg, latestDaysAgo + it) }

    private fun suggest(events: List<ChoiceEvent>, dismissed: Set<String> = emptySet()) =
        UninstallSuggestions.suggest(events, now, dismissed)

    @Test
    fun `an app always refused enough times is suggested`() {
        assertEquals(listOf(UninstallSuggestion("a", 10, 10)), suggest(refusals("a", 10)))
    }

    @Test
    fun `no answer, no suggestion`() {
        assertTrue(suggest(emptyList()).isEmpty())
    }

    @Test
    fun `too few attempts say nothing yet`() {
        assertTrue(suggest(refusals("a", 9)).isEmpty())
    }

    @Test
    fun `a share of refusals just at the threshold is enough, just below is not`() {
        val ninety = refusals("a", 9) + event("a", 20.0, proceeded = true)
        assertEquals(1, suggest(ninety).size)
        val below = refusals("a", 8) + event("a", 20.0, proceeded = true) + event("a", 21.0, proceeded = true)
        assertTrue(suggest(below).isEmpty())
    }

    @Test
    fun `a pause counts as a way through, not as a refusal`() {
        val events = refusals("a", 8) +
            event("a", 20.0, proceeded = true, snoozed = true) +
            event("a", 21.0, proceeded = true, snoozed = true)
        assertTrue(suggest(events).isEmpty())
    }

    @Test
    fun `answers older than the window are forgotten`() {
        val old = refusals("a", 20, latestDaysAgo = 91.0)
        assertTrue(suggest(old).isEmpty())
        val recentAndOld = refusals("a", 5) + refusals("a", 20, latestDaysAgo = 91.0)
        assertTrue(suggest(recentAndOld).isEmpty())
    }

    @Test
    fun `an app no longer tried for a month is not suggested`() {
        assertTrue(suggest(refusals("a", 10, latestDaysAgo = 31.0)).isEmpty())
        assertEquals(1, suggest(refusals("a", 10, latestDaysAgo = 29.0)).size)
    }

    @Test
    fun `a recent Yes means the app is still useful`() {
        val events = refusals("a", 20, latestDaysAgo = 2.0) + event("a", 0.5, proceeded = true)
        assertTrue(suggest(events).isEmpty())
    }

    @Test
    fun `an old Yes does not prevent the suggestion once the last answers are refusals`() {
        val events = refusals("a", 20) + event("a", 30.0, proceeded = true)
        assertEquals(UninstallSuggestion("a", 21, 20), suggest(events).single())
    }

    @Test
    fun `a dismissed app is never suggested again`() {
        assertTrue(suggest(refusals("a", 10), dismissed = setOf("a")).isEmpty())
    }

    @Test
    fun `each app is judged on its own answers`() {
        val events = refusals("a", 10) + refusals("b", 5) + (0 until 10).map { event("c", 1.0 + it, proceeded = true) }
        assertEquals(listOf("a"), suggest(events).map { it.packageName })
    }

    @Test
    fun `the most refused app comes first, then the most tried`() {
        val mostlyRefused = refusals("a", 18) + event("a", 40.0, proceeded = true) + event("a", 41.0, proceeded = true)
        val alwaysRefused = refusals("b", 10)
        val alwaysRefusedMore = refusals("c", 15)
        val result = suggest(mostlyRefused + alwaysRefused + alwaysRefusedMore)
        assertEquals(listOf("c", "b", "a"), result.map { it.packageName })
    }

    @Test
    fun `the order of the stored answers does not matter`() {
        val events = refusals("a", 20) + event("a", 0.5, proceeded = true)
        assertTrue(suggest(events.shuffled(java.util.Random(1))).isEmpty())
    }

    @Test
    fun `rules can be tuned`() {
        val rules = UninstallRules(minAttempts = 3, minRefusalShare = 1.0)
        assertEquals(1, UninstallSuggestions.suggest(refusals("a", 3), now, rules = rules).size)
        val oneYes = refusals("a", 5) + event("a", 20.0, proceeded = true)
        assertTrue(UninstallSuggestions.suggest(oneYes, now, rules = rules).isEmpty())
    }

    @Test
    fun `the rules never look further back than the answers kept one by one`() {
        assertTrue(UninstallRules().windowMillis <= HistoryCompaction.KEEP_MILLIS)
    }
}
