package app.redflag.service

/**
 * Remembers the user's answers to the interruption screen, so as not to ask again wrongly.
 *
 * - "Yes": the app stays allowed for [ALLOW_MILLIS] (15 minutes) after its last time in the foreground. Leaving the app, even by
 *   mistake, then coming back within that time asks nothing; each visit of the app extends the permission while it is used.
 *   Several apps can be allowed at the same time. The permission is forgotten when the screen turns off.
 * - "No": the app is sent back to the home screen; its stray events right after are ignored, otherwise the interruption screen
 *   would open again at once, in a loop. The protection ends as soon as another app (the launcher) has been in the foreground
 *   for [DECLINE_SETTLE_MILLIS], or after [DECLINE_GUARD_MILLIS] at the latest: opening the app again after a "No" asks again,
 *   every time. A "No" removes a previous "Yes".
 *
 * [now] can be injected to test the time.
 */
class FrictionGate(private val now: () -> Long = System::currentTimeMillis) {
    /** For each allowed app, the moment its "Yes" stops being valid. */
    private val allowedUntil = HashMap<String, Long>()
    private var declined: String? = null
    private var declinedUntil = 0L

    /** When another app came to the foreground after the "No" (the launcher, normally); 0 = not yet. */
    private var elsewhereSince = 0L

    @Synchronized
    fun allow(packageName: String) {
        allowedUntil[packageName] = now() + ALLOW_MILLIS
    }

    /** True if [packageName] got a "Yes" that is still valid. Each visit extends the permission (the app is being used). */
    @Synchronized
    fun isAllowed(packageName: String): Boolean {
        val until = allowedUntil[packageName] ?: return false
        val time = now()
        if (time >= until) {
            allowedUntil.remove(packageName)
            return false
        }
        allowedUntil[packageName] = time + ALLOW_MILLIS
        return true
    }

    /** The screen turns off: the next return to an app must ask again. */
    @Synchronized
    fun revokeAll() {
        allowedUntil.clear()
    }

    @Synchronized
    fun declined(packageName: String) {
        declined = packageName
        declinedUntil = now() + DECLINE_GUARD_MILLIS
        elsewhereSince = 0L
        allowedUntil.remove(packageName)
    }

    /** Another app than the declined one came to the foreground (the launcher after a "No"): the stray events will soon be over. */
    @Synchronized
    fun otherAppInForeground(packageName: String) {
        if (declined != null && declined != packageName && elsewhereSince == 0L) elsewhereSince = now()
    }

    /**
     * True while the stray events of [packageName] just after a "No" must be ignored: not once the user has been elsewhere for
     * [DECLINE_SETTLE_MILLIS], so that opening the app again asks again.
     */
    @Synchronized
    fun isDeclineGuarded(packageName: String): Boolean {
        if (declined != packageName) return false
        val time = now()
        if (time >= declinedUntil || (elsewhereSince != 0L && time - elsewhereSince >= DECLINE_SETTLE_MILLIS)) {
            declined = null
            return false
        }
        return true
    }

    companion object {
        /** How long a "Yes" lasts after the last visit of the app (a window event of the app extends it). */
        const val ALLOW_MILLIS = 15 * 60_000L

        /** After a "No", the events of the same app are ignored for this long at most. */
        const val DECLINE_GUARD_MILLIS = 3_000L

        /** After a "No", the events of the same app stop being ignored once another app has been in the foreground this long. */
        const val DECLINE_SETTLE_MILLIS = 700L
    }
}
