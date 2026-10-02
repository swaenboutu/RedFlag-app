package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.container
import fr.conscience.numerique.data.InstalledApp
import fr.conscience.numerique.data.Problem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Les problématiques restent des références : leur libellé est résolu à l'affichage, dans la langue courante.
 * [first] et [last] servent à arrondir la carte blanche qui regroupe toutes les lignes.
 */
data class AppItem(
    val app: InstalledApp,
    val problems: List<Problem>,
    val overrides: Map<String, String>,
    val first: Boolean = false,
    val last: Boolean = false,
)

enum class AppFilter { ALL, FLAGGED, UNFLAGGED }

data class AppsState(
    val items: List<AppItem> = emptyList(),
    val flaggedCount: Int = 0,
    val filter: AppFilter = AppFilter.ALL,
    /** Faux tant que la liste des apps installées n'est pas chargée (évite d'afficher « aucun résultat » trop tôt). */
    val loaded: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container
    private val installed = MutableStateFlow<List<InstalledApp>?>(null)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppFilter.ALL)

    val state: StateFlow<AppsState> = combine(
        installed,
        container.repository.monitoredApps,
        container.repository.labelOverrides,
        query,
        filter,
    ) { apps, monitored, overrides, query, filter ->
        if (apps == null) return@combine AppsState(filter = filter)

        val problemsByPackage = monitored.associate { it.app.packageName to it.problems }
        val all = apps.map { AppItem(it, problemsByPackage[it.packageName].orEmpty(), overrides) }
        val visible = all
            .filter { query.isBlank() || it.app.label.contains(query.trim(), ignoreCase = true) }
            .filter {
                when (filter) {
                    AppFilter.ALL -> true
                    AppFilter.FLAGGED -> it.problems.isNotEmpty()
                    AppFilter.UNFLAGGED -> it.problems.isEmpty()
                }
            }
            // Apps signalées d'abord, puis ordre alphabétique.
            .sortedWith(compareByDescending<AppItem> { it.problems.isNotEmpty() }.thenBy { it.app.label.lowercase() })
            .mapIndexed { index, item -> item.copy(first = index == 0, last = false) }
            .let { list -> list.mapIndexed { index, item -> item.copy(last = index == list.lastIndex) } }

        AppsState(visible, all.count { it.problems.isNotEmpty() }, filter, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsState())

    init {
        // Recharge la liste quand le réglage « Liste affichée » ou les apps signalées changent.
        viewModelScope.launch { container.installedAppsFlow().collect { installed.value = it } }
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun setFilter(value: AppFilter) {
        filter.value = value
    }
}
