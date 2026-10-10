package app.redflag.ui

import app.redflag.data.ChoiceEvent
import app.redflag.data.MonitoredApp
import app.redflag.data.MonitoredAppWithProblems
import app.redflag.data.Problem
import app.redflag.data.ProblemRef
import app.redflag.ui.home.Squares
import app.redflag.ui.home.squares
import app.redflag.ui.home.topInterruptedApp
import app.redflag.ui.home.topIssue
import app.redflag.util.PauseChoice
import app.redflag.util.nextMorning
import app.redflag.util.resumeAt
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the home screen shows: the most interrupted app, the most present issue, and the pause durations. */
class HomeStatsTest {
    private fun app(pkg: String, vararg problems: String) =
        MonitoredAppWithProblems(MonitoredApp(pkg, pkg), problems.map { Problem(packageName = pkg, problemId = it) })

    private fun events(vararg counts: Pair<String, Int>) =
        counts.flatMap { (pkg, n) -> List(n) { ChoiceEvent(packageName = pkg, timestamp = 1L, proceeded = it % 2 == 0) } }

    @Test
    fun theMostInterruptedAppIsTheOneThatInterruptedTheMost() {
        val top = topInterruptedApp(events("a" to 3, "b" to 18, "c" to 7), mapOf("a" to "Alpha", "b" to "Beta"))

        assertEquals("b", top?.packageName)
        assertEquals("Beta", top?.label)
        assertEquals("every answer counts, \"yes\" as well as \"no\"", 18, top?.interruptions)
    }

    @Test
    fun onATieTheFirstAppInAlphabeticalOrderWins() {
        val top = topInterruptedApp(events("z" to 5, "m" to 5, "a" to 5), mapOf("z" to "Zebra", "m" to "Mango", "a" to "Álamo"))

        assertEquals("Álamo", top?.label)
    }

    @Test
    fun anAppNoLongerFlaggedIsNamedByItsPackage() {
        assertEquals("old.app", topInterruptedApp(events("old.app" to 2), emptyMap())?.label)
    }

    @Test
    fun withNoInterruptionThereIsNoTopApp() {
        assertNull(topInterruptedApp(emptyList(), emptyMap()))
    }

    @Test
    fun theMostPresentIssueIsTheOneLinkedToTheMostApps() {
        val apps = listOf(app("a", "fomo", "sexism"), app("b", "fomo"), app("c", "fomo", "racism"), app("d", "sexism"))

        val top = topIssue(apps) { it.id.uppercase() }

        assertEquals(ProblemRef("fomo"), top?.ref)
        assertEquals(3, top?.apps)
        assertEquals("FOMO", top?.label)
    }

    @Test
    fun aSecondLinkOfTheSameIssueToTheSameAppCountsOnce() {
        val apps = listOf(app("a", "fomo", "fomo", "fomo"), app("b", "sexism"), app("c", "sexism"))

        assertEquals(ProblemRef("sexism"), topIssue(apps) { it.id }?.ref)
    }

    @Test
    fun onATieTheFirstIssueInAlphabeticalOrderWins() {
        val apps = listOf(app("a", "fomo", "sexism"), app("b", "fomo", "sexism"))

        assertEquals(ProblemRef("fomo"), topIssue(apps) { if (it.id == "fomo") "Peur de rater" else "Sexisme" }?.ref)
    }

    @Test
    fun withNoFlaggedAppThereIsNoTopIssue() {
        assertNull(topIssue(emptyList()) { it.id })
    }

    @Test
    fun theBarHasOneSquarePerAppUpToTen() {
        assertEquals(Squares(8, 5), squares(apps = 5, ofTotal = 8))
        assertEquals(Squares(0, 0), squares(apps = 0, ofTotal = 0))
    }

    @Test
    fun withMoreThanTenAppsEachSquareStandsForSeveral() {
        assertEquals(Squares(10, 5), squares(apps = 15, ofTotal = 30))
        assertEquals("never empty for a non-zero count", Squares(10, 1), squares(apps = 1, ofTotal = 100))
    }

    private val paris = TimeZone.getTimeZone("Europe/Paris")

    private fun at(hour: Int, minute: Int = 0): Long = Calendar.getInstance(paris).apply {
        set(2026, Calendar.OCTOBER, 12, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun hourOf(millis: Long): Pair<Int, Int> = Calendar.getInstance(paris).apply { timeInMillis = millis }.let { it.get(Calendar.DAY_OF_MONTH) to it.get(Calendar.HOUR_OF_DAY) }

    @Test
    fun theMorningPauseEndsAtEightTodayIfItIsEarlierThanThat() {
        assertEquals(12 to 8, hourOf(nextMorning(at(6, 30), paris)))
    }

    @Test
    fun theMorningPauseEndsAtEightTomorrowOtherwise() {
        assertEquals(13 to 8, hourOf(nextMorning(at(9), paris)))
        assertEquals("exactly eight is already over", 13 to 8, hourOf(nextMorning(at(8), paris)))
        assertEquals(13 to 8, hourOf(nextMorning(at(23, 59), paris)))
    }

    @Test
    fun pausesInMinutesEndLaterAndTheLastOneNeverEnds() {
        val now = at(10)

        assertEquals(now + 15 * 60_000L, PauseChoice.Minutes(15).resumeAt(now, paris))
        assertEquals(now + 4 * 3_600_000L, PauseChoice.Minutes(240).resumeAt(now, paris))
        assertNull(PauseChoice.UntilTurnedBackOn.resumeAt(now, paris))
    }

    @Test
    fun theSheetOffersFiveChoicesAndStartsOnOneHour() {
        assertEquals(5, PauseChoice.ALL.size)
        assertEquals(PauseChoice.Minutes(60), PauseChoice.DEFAULT)
    }
}
