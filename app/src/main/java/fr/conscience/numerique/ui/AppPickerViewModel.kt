package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.ConscienceApp
import fr.conscience.numerique.data.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AppPickerArgs {
    const val EXCLUDED = "excluded"
}

data class AppPickRow(val app: InstalledApp, val checked: Boolean)

class AppPickerViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val container = (application as ConscienceApp).container
    private val excluded: Set<String> = handle.get<ArrayList<String>>(AppPickerArgs.EXCLUDED).orEmpty().toSet()

    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val query = MutableStateFlow("")
    private val selected = MutableStateFlow<Set<String>>(emptySet())

    val rows: StateFlow<List<AppPickRow>> = combine(apps, query, selected) { apps, query, selected ->
        apps.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            .map { AppPickRow(it, it.packageName in selected) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedCount: StateFlow<Int> = selected.combine(apps) { selected, _ -> selected.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            apps.value = container.installedApps.list()
                .filter { it.packageName !in excluded }
                .sortedBy { it.label.lowercase() }
        }
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun toggle(packageName: String) = selected.update { if (packageName in it) it - packageName else it + packageName }

    fun selectedApps(): List<InstalledApp> = apps.value.filter { it.packageName in selected.value }
}
