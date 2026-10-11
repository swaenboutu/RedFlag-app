package app.redflag.ui.home

import app.redflag.data.ChoiceEvent
import app.redflag.data.MonitoredAppWithProblems
import app.redflag.data.ProblemRef
import app.redflag.util.alphabetical

/** The app that interrupted the user the most: [interruptions] of them, whatever the answer. */
data class TopApp(val packageName: String, val label: String, val interruptions: Int)

/** The issue linked to the most flagged apps: [apps] of them. */
data class TopIssue(val ref: ProblemRef, val label: String, val apps: Int)

/**
 * The app that interrupted the user the most (each recorded choice is one interruption, whatever the answer); on a tie, the first
 * one in alphabetical order. [names]: the name of each app by package (an app that is no longer flagged keeps its package name).
 */
fun topInterruptedApp(events: List<ChoiceEvent>, names: Map<String, String>): TopApp? =
    events
        .groupingBy { it.packageName }
        .eachCount()
        .map { (pkg, count) -> TopApp(pkg, names[pkg] ?: pkg, count) }
        .sortedWith(compareByDescending<TopApp> { it.interruptions }.thenBy(alphabetical()) { it.label })
        .firstOrNull()

/**
 * The issue linked to the most apps (each app counts once per issue); on a tie, the first one in alphabetical order of its
 * label. [labelOf]: the label shown for an issue.
 */
fun topIssue(apps: List<MonitoredAppWithProblems>, labelOf: (ProblemRef) -> String): TopIssue? {
    val counts = apps
        .flatMap { app -> app.problems.map { it.problemId }.distinct() }
        .groupingBy { it }
        .eachCount()
    return counts
        .map { (id, count) -> TopIssue(ProblemRef(id), labelOf(ProblemRef(id)), count) }
        .sortedWith(compareByDescending<TopIssue> { it.apps }.thenBy(alphabetical()) { it.label })
        .firstOrNull()
}
