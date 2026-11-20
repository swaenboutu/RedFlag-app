package app.redflag.data

import kotlin.random.Random

/**
 * Fake answers to the interruption screen, for the debug menu: fills the statistics without having to open apps for weeks.
 * Each app gets a [Habit] (how often the user refuses, opens or pauses it) and answers spread over the last [WINDOW_MILLIS].
 */
object SampleData {
    /** How the user answers for an app: [refusalShare] of "No"; among the other answers, [snoozeShare] of pauses. */
    enum class Habit(val refusalShare: Double, val snoozeShare: Double, val minAttempts: Int, val maxAttempts: Int) {
        /** Always refused, and recently: enough for an uninstall suggestion (see [UninstallSuggestions]). */
        ALWAYS_REFUSED(1.0, 0.0, 15, 35),
        MOSTLY_REFUSED(0.75, 0.2, 10, 40),
        MIXED(0.5, 0.25, 10, 50),
        MOSTLY_OPENED(0.2, 0.2, 5, 30),
    }

    /** The answers are spread over the same window as the uninstall suggestions: 3 months. */
    val WINDOW_MILLIS = UninstallRules().windowMillis

    /** The latest answer of an [Habit.ALWAYS_REFUSED] app is at most this old, so the suggestion shows up. */
    private const val RECENT_MILLIS = 2 * UninstallRules.DAY_MILLIS

    /**
     * Answers for each of [packages], between [now] minus [WINDOW_MILLIS] and [now], oldest first. The first app is always
     * [Habit.ALWAYS_REFUSED] (so there is something to suggest), the others get a random habit. [random] is injected for the tests.
     */
    fun events(packages: List<String>, now: Long, random: Random = Random.Default): List<ChoiceEvent> =
        packages.flatMapIndexed { index, pkg ->
            val habit = if (index == 0) Habit.ALWAYS_REFUSED else Habit.entries.random(random)
            answers(pkg, habit, now, random)
        }.sortedBy { it.timestamp }

    fun answers(packageName: String, habit: Habit, now: Long, random: Random): List<ChoiceEvent> {
        val count = random.nextInt(habit.minAttempts, habit.maxAttempts + 1)
        val times = MutableList(count) { now - random.nextLong(WINDOW_MILLIS) }
        // A recent answer: an app that is still being refused today (otherwise the suggestion would not show).
        if (habit == Habit.ALWAYS_REFUSED) times[0] = now - random.nextLong(RECENT_MILLIS)
        return times.map { time ->
            val refused = random.nextDouble() < habit.refusalShare
            ChoiceEvent(
                packageName = packageName,
                timestamp = time,
                proceeded = !refused,
                snoozed = !refused && random.nextDouble() < habit.snoozeShare,
            )
        }
    }
}
