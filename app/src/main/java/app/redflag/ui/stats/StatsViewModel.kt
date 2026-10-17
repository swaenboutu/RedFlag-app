package app.redflag.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.redflag.AppContainer
import app.redflag.util.alphabetical
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An app with an interruption in place, and how many times the interruption was shown for it. */
data class StatsApp(val packageName: String, val appName: String, val attempts: Int)

data class StatsRow(val app: StatsApp, val first: Boolean, val last: Boolean)

/** [installedCount]: null until the installed apps are counted. */
data class StatsState(val installedCount: Int?, val interceptedCount: Int, val rows: List<StatsRow>)

/** "Statistics" screen: how many apps have an interruption, and the list of them. */
class StatsViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository

    private val installedCount = MutableStateFlow<Int?>(null)

    val state: StateFlow<StatsState> = combine(
        repository.monitoredApps,
        repository.choiceEvents,
        installedCount,
    ) { monitored, events, installed ->
        val attempts = events.groupingBy { it.packageName }.eachCount()
        val apps = monitored
            .map { StatsApp(it.app.packageName, it.app.appName, attempts[it.app.packageName] ?: 0) }
            .sortedWith(compareByDescending<StatsApp> { it.attempts }.thenBy(alphabetical()) { it.appName })
        StatsState(
            installedCount = installed,
            interceptedCount = apps.size,
            rows = apps.mapIndexed { i, app -> StatsRow(app, first = i == 0, last = i == apps.lastIndex) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState(null, 0, emptyList()))

    init {
        // The total counts the same apps as the list of apps (a flagged app is always in it), without loading their icons.
        viewModelScope.launch { container.installedAppsCountFlow().collect { installedCount.value = it } }
    }
}
