package app.redflag.data

/**
 * An app the user almost always refuses to open: suggesting to uninstall it may help. [attempts] and [refusals] are counted
 * over the [UninstallRules.windowMillis] window.
 */
data class UninstallSuggestion(val packageName: String, val attempts: Int, val refusals: Int) {
    val refusalShare: Double get() = refusals.toDouble() / attempts
}

/**
 * The rules that decide when an app is worth an uninstall suggestion. All the data stays on the phone: only the recorded
 * answers to the interruption screen ([ChoiceEvent]) are used.
 *
 * - Only the answers of the last [windowMillis] count (a sliding window: old habits are forgotten).
 * - The app needs at least [minAttempts] answers in that window: a few refusals say nothing yet.
 * - At least [minRefusalShare] of them are a "No". A pause ("Don't ask again for…") is a way through, so it counts as a "Yes".
 * - The app is still refused today: its last [recentStreak] answers are all a "No", and the latest one is at most
 *   [recentMillis] old (an app that is no longer even tried does not need a suggestion).
 */
data class UninstallRules(
    val windowMillis: Long = 90 * DAY_MILLIS,
    val minAttempts: Int = 10,
    val minRefusalShare: Double = 0.9,
    val recentStreak: Int = 3,
    val recentMillis: Long = 30 * DAY_MILLIS,
) {
    init {
        require(minAttempts >= 1 && recentStreak >= 1) { "minAttempts and recentStreak must be at least 1" }
        require(minRefusalShare in 0.0..1.0) { "minRefusalShare must be between 0 and 1" }
    }

    companion object {
        const val DAY_MILLIS = 24 * 60 * 60_000L
    }
}

object UninstallSuggestions {
    /**
     * The apps to suggest uninstalling, the most refused first (then the most tried, then by package name, so the order is
     * stable). [dismissed]: the apps for which the user chose "Don't suggest this again". [now] is injected for the tests.
     */
    fun suggest(
        events: List<ChoiceEvent>,
        now: Long,
        dismissed: Set<String> = emptySet(),
        rules: UninstallRules = UninstallRules(),
    ): List<UninstallSuggestion> {
        val windowStart = now - rules.windowMillis
        return events
            .asSequence()
            .filter { it.timestamp in windowStart..now && it.packageName !in dismissed }
            .groupBy { it.packageName }
            .mapNotNull { (pkg, appEvents) -> evaluate(pkg, appEvents.sortedBy { it.timestamp }, now, rules) }
            .sortedWith(
                compareByDescending<UninstallSuggestion> { it.refusalShare }
                    .thenByDescending { it.attempts }
                    .thenBy { it.packageName },
            )
    }

    /** [events]: the answers for [packageName] inside the window, oldest first. */
    private fun evaluate(packageName: String, events: List<ChoiceEvent>, now: Long, rules: UninstallRules): UninstallSuggestion? {
        if (events.size < rules.minAttempts) return null
        val refusals = events.count { !it.proceeded }
        val suggestion = UninstallSuggestion(packageName, events.size, refusals)
        if (suggestion.refusalShare < rules.minRefusalShare) return null
        if (now - events.last().timestamp > rules.recentMillis) return null
        if (events.takeLast(rules.recentStreak).any { it.proceeded }) return null
        return suggestion
    }
}
