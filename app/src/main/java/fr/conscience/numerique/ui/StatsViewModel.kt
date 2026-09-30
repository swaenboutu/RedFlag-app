package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.container
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Une app avec une interruption en place, et le nombre de fois où l'interruption s'est affichée pour elle. */
data class StatsApp(val packageName: String, val appName: String, val attempts: Int)

data class StatsRow(val app: StatsApp, val first: Boolean, val last: Boolean)

/** [installedCount] : null tant que la liste des apps installées n'est pas chargée. */
data class StatsState(val installedCount: Int?, val interceptedCount: Int, val rows: List<StatsRow>)

/** Écran « Statistiques » : combien d'apps ont une interruption, et la liste de celles-ci. */
class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container
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
            .sortedWith(compareByDescending<StatsApp> { it.attempts }.thenBy { it.appName.lowercase() })
        StatsState(
            installedCount = installed,
            interceptedCount = apps.size,
            rows = apps.mapIndexed { i, app -> StatsRow(app, first = i == 0, last = i == apps.lastIndex) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState(null, 0, emptyList()))

    init {
        viewModelScope.launch {
            installedCount.value = withContext(Dispatchers.IO) {
                container.installedApps.list(container.settings.hideSystemApps.value).size
            }
        }
    }
}
