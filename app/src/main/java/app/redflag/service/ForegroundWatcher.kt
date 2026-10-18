package app.redflag.service

/** One usage event as the detection needs it: which app, what happened, when. Built from `UsageEvents.Event`, or by hand in tests. */
data class UsageEventLite(val packageName: String, val type: Int, val timeStamp: Long)

/**
 * Turns the usage events read at each poll into decisions, with the same rules as the accessibility version: no Android call, so it
 * is fed with fake events in the unit tests.
 *
 * - Only the "activity resumed" events count (an app coming to the foreground). Several resumes of the same app in a row (moving
 *   from one of its screens to another) are one single arrival.
 * - Our own screens, the system UI and the input methods are never interrupted, and do not count as "another app" for the gate;
 *   they still end the current app's stay in the foreground (coming back to it is a new arrival, as with the accessibility events).
 * - Every other app in the foreground tells the gate the user is elsewhere ([FrictionGate.otherAppInForeground]).
 * - Only the app in the foreground at the end of a poll can be interrupted: an app opened then left within one poll is not.
 * - While an allowed app stays in the foreground, each poll extends its "Yes" ([keepAlive]), as its window events did before.
 * - Until our interruption screen has come to the foreground, every new screen of the app to interrupt asks again: an app that opens
 *   a second screen right after starting (Chrome's welcome screen, a splash screen) would otherwise cover the interruption.
 *
 * [monitored] gives, for each flagged app, the end of its "Don't ask again" pause (0 = none). [now] can be injected for the tests.
 */
class ForegroundWatcher(
    private val ownPackage: String,
    private val gate: FrictionGate,
    private val isIgnored: (String) -> Boolean,
    private val monitored: () -> Map<String, Long>,
    private val isActive: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** The app in the foreground as far as the events tell; null when unknown (start, screen off). */
    var current: String? = null
        private set

    /** The app the interruption screen was asked for, until that screen (or another app) comes to the foreground. */
    private var pending: String? = null

    /** Time stamp of the last event already handled: the next poll starts there. */
    var lastTimeStamp: Long = 0L
        private set

    /** First read when the service starts: learns which app is in front without interrupting it (it was opened before). */
    fun prime(events: List<UsageEventLite>, upTo: Long) {
        arrivals(events).lastOrNull()?.let { current = it }
        lastTimeStamp = maxOf(lastTimeStamp, events.maxOfOrNull { it.timeStamp } ?: 0L, upTo)
    }

    /** Handles the events of one poll; returns the app to interrupt, or null. */
    fun onEvents(events: List<UsageEventLite>): String? {
        val arrivals = arrivals(events)
        events.maxOfOrNull { it.timeStamp }?.let { lastTimeStamp = maxOf(lastTimeStamp, it) }
        if (arrivals.isEmpty()) {
            keepAlive()
            return null
        }
        for (pkg in arrivals) {
            current = pkg
            if (pkg != pending) pending = null
            if (!isOwnOrIgnored(pkg)) gate.otherAppInForeground(pkg)
        }
        val pkg = arrivals.last()
        pending = if (shouldInterrupt(pkg)) pkg else null
        return pending
    }

    /** The screen turns off: every "Yes" is forgotten, and the app found in front when it turns back on is a new arrival. */
    fun screenOff() {
        current = null
        pending = null
        gate.revokeAll()
    }

    /** The apps that came to the foreground, oldest first, without repeats of the app already in front (unless it is [pending]). */
    private fun arrivals(events: List<UsageEventLite>): List<String> {
        val out = ArrayList<String>()
        var last = current
        for (event in events.sortedBy { it.timeStamp }) {
            if (event.type != ACTIVITY_RESUMED || event.timeStamp <= lastTimeStamp) continue
            if (event.packageName == last && event.packageName != pending) continue
            out += event.packageName
            last = event.packageName
        }
        return out
    }

    private fun shouldInterrupt(pkg: String): Boolean {
        if (isOwnOrIgnored(pkg) || !isActive()) return false
        val snoozedUntil = monitored()[pkg] ?: return false
        if (gate.isDeclineGuarded(pkg)) return false
        return !gate.isAllowed(pkg) && snoozedUntil <= now()
    }

    /** The allowed app is still in front: its "Yes" lasts [FrictionGate.ALLOW_MILLIS] after it is left, not after it was opened. */
    private fun keepAlive() {
        val pkg = current ?: return
        if (pkg in monitored()) gate.isAllowed(pkg)
    }

    private fun isOwnOrIgnored(pkg: String) = pkg == ownPackage || isIgnored(pkg)

    companion object {
        /** `UsageEvents.Event.ACTIVITY_RESUMED` (Android 10+), same value as `MOVE_TO_FOREGROUND` before. */
        const val ACTIVITY_RESUMED = 1
    }
}
