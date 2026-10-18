package app.redflag.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The foreground detection from usage events, fed with fake events: the same rules as the accessibility version. */
class ForegroundWatcherTest {
    private var time = 1_000_000L
    private val gate = FrictionGate { time }
    private var monitored = mapOf(FLAGGED to 0L)
    private var active = true
    private val watcher = ForegroundWatcher(
        ownPackage = OWN,
        gate = gate,
        isIgnored = { it == SYSTEM_UI || it == KEYBOARD },
        monitored = { monitored },
        isActive = { active },
        now = { time },
    )

    /** One poll: the given apps come to the foreground one after the other, then [POLL] passes. */
    private fun poll(vararg packages: String, type: Int = ForegroundWatcher.ACTIVITY_RESUMED): String? {
        val events = packages.map { time += 10; UsageEventLite(it, type, time) }
        time += POLL
        return watcher.onEvents(events)
    }

    @Test
    fun `a flagged app coming to the foreground is interrupted`() {
        assertEquals(FLAGGED, poll(FLAGGED))
    }

    @Test
    fun `an app that is not flagged is not interrupted`() {
        assertNull(poll(OTHER))
    }

    @Test
    fun `only resume events count`() {
        assertNull(poll(FLAGGED, type = 2)) // ACTIVITY_PAUSED
    }

    @Test
    fun `an event already handled is not handled twice`() {
        val event = UsageEventLite(FLAGGED, ForegroundWatcher.ACTIVITY_RESUMED, time)
        assertEquals(FLAGGED, watcher.onEvents(listOf(event)))
        poll(LAUNCHER)
        assertNull("the same event read again by the next query", watcher.onEvents(listOf(event)))
    }

    @Test
    fun `moving between screens of the same app is one arrival`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        gate.allow(FLAGGED)
        assertNull(poll(FLAGGED, FLAGGED))
    }

    @Test
    fun `only the app in front at the end of a poll is interrupted`() {
        assertNull("opened then left within one poll", poll(FLAGGED, LAUNCHER))
        assertEquals("left for the flagged app within one poll", FLAGGED, poll(LAUNCHER, FLAGGED))
    }

    @Test
    fun `an interruption covered by the app's next screen is asked again until it shows`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        assertEquals("the app opened a second screen over the interruption", FLAGGED, poll(FLAGGED))
        poll(OWN) // the interruption screen is in front at last
        gate.allow(FLAGGED)
        assertNull(poll(FLAGGED))
        assertNull("allowed: another screen of the app asks nothing", poll(FLAGGED))
    }

    @Test
    fun `nothing is interrupted while the interruption is paused, or the app snoozed`() {
        active = false
        assertNull(poll(FLAGGED))
        active = true
        poll(LAUNCHER)
        monitored = mapOf(FLAGGED to time + 60_000L)
        assertNull(poll(FLAGGED))
        poll(LAUNCHER)
        time += 60_000L
        assertEquals("the snooze is over", FLAGGED, poll(FLAGGED))
    }

    @Test
    fun `our own screens, the system UI and the keyboard are never interrupted`() {
        monitored = mapOf(FLAGGED to 0L, OWN to 0L, SYSTEM_UI to 0L, KEYBOARD to 0L)
        assertNull(poll(OWN))
        assertNull(poll(SYSTEM_UI))
        assertNull(poll(KEYBOARD))
    }

    @Test
    fun `after a Yes the app opens freely, after leaving it too`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        poll(OWN) // the interruption screen
        gate.allow(FLAGGED)
        assertNull("back to the app after Yes", poll(FLAGGED))
        poll(LAUNCHER)
        assertNull("opened again a little later", poll(FLAGGED))
    }

    @Test
    fun `the Yes lasts while the app stays in front, and expires once it has been left long enough`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        gate.allow(FLAGGED)
        poll(FLAGGED)
        // 20 minutes in the app without any new event: every poll extends the Yes.
        repeat((20 * 60_000L / POLL).toInt()) { poll() }
        poll(LAUNCHER)
        assertNull("left just now: still allowed", poll(FLAGGED))
        poll(LAUNCHER)
        repeat(((FrictionGate.ALLOW_MILLIS + 60_000L) / POLL).toInt()) { poll() }
        assertEquals("left more than 15 minutes ago", FLAGGED, poll(FLAGGED))
    }

    @Test
    fun `leaving the interruption screen without answering and going back to the app asks again`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        poll(OWN)
        assertEquals(FLAGGED, poll(FLAGGED))
    }

    @Test
    fun `after a No the stray events are ignored, then opening the app again asks again`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        poll(OWN)
        gate.declined(FLAGGED)
        // Sent to the home screen, the app may come back for an instant.
        assertNull("stray event right after the No", poll(LAUNCHER, FLAGGED))
        poll(LAUNCHER)
        time += FrictionGate.DECLINE_SETTLE_MILLIS
        assertEquals("opened again from the launcher", FLAGGED, poll(FLAGGED))
    }

    @Test
    fun `the screen turning off forgets the Yes, and the app found in front is a new arrival`() {
        assertEquals(FLAGGED, poll(FLAGGED))
        gate.allow(FLAGGED)
        poll(FLAGGED)
        watcher.screenOff()
        assertEquals("unlocking back into the app asks again", FLAGGED, poll(FLAGGED))
    }

    @Test
    fun `at start the app already in front is learned, not interrupted`() {
        watcher.prime(listOf(UsageEventLite(FLAGGED, ForegroundWatcher.ACTIVITY_RESUMED, time - 5_000)), time)
        assertEquals(FLAGGED, watcher.current)
        assertNull(poll())
        assertNull("another screen of the same app", poll(FLAGGED))
        poll(LAUNCHER)
        assertEquals(FLAGGED, poll(FLAGGED))
    }

    private companion object {
        const val POLL = 500L
        const val OWN = "app.redflag.android"
        const val FLAGGED = "com.example.flagged"
        const val OTHER = "com.example.other"
        const val LAUNCHER = "com.example.launcher"
        const val SYSTEM_UI = "com.android.systemui"
        const val KEYBOARD = "com.example.keyboard"
    }
}
