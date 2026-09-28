package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.ConscienceApp
import fr.conscience.numerique.data.InstalledApp
import fr.conscience.numerique.data.Problem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Les problématiques restent des références : leur libellé est résolu à l'affichage, dans la langue courante. */
data class AppItem(val app: InstalledApp, val problems: List<Problem>, val overrides: Map<String, String>)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ConscienceApp).container
    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())

    /** Apps labelisées en premier, puis ordre alphabétique. */
    val items: StateFlow<List<AppItem>> = combine(
        installed,
        container.repository.monitoredApps,
        container.repository.labelOverrides,
    ) { apps, monitored, overrides ->
        val problemsByPackage = monitored.associate { it.app.packageName to it.problems }
        apps.map { AppItem(it, problemsByPackage[it.packageName].orEmpty(), overrides) }
            .sortedWith(compareByDescending<AppItem> { it.problems.isNotEmpty() }.thenBy { it.app.label.lowercase() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) { installed.value = container.installedApps.list() }
    }
}
