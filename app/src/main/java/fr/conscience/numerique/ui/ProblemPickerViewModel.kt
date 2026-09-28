package fr.conscience.numerique.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.ConscienceApp
import fr.conscience.numerique.R
import fr.conscience.numerique.data.PredefinedProblem
import fr.conscience.numerique.data.ProblemCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Clés des extras de l'intent, lues aussi par le SavedStateHandle du ViewModel. */
object PickerArgs {
    const val PACKAGE = "package"
    const val LABEL = "label"
}

sealed interface PickerRow {
    data class Header(val emoji: String, @StringRes val title: Int) : PickerRow
    data class Predefined(val problem: PredefinedProblem, val checked: Boolean, val override: String?) : PickerRow
    data class Custom(val text: String, val checked: Boolean) : PickerRow
}

class ProblemPickerViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = (application as ConscienceApp).container.repository
    private val packageName: String = checkNotNull(handle[PickerArgs.PACKAGE])
    private val appName: String = handle[PickerArgs.LABEL] ?: packageName

    private val selectedKeys = MutableStateFlow<Set<String>>(emptySet())
    private val selectedCustom = MutableStateFlow<Set<String>>(emptySet())

    private val knownCustom: StateFlow<List<String>> =
        repository.customLabels.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rows: StateFlow<List<PickerRow>> =
        combine(selectedKeys, selectedCustom, knownCustom, repository.labelOverrides) { keys, custom, known, overrides ->
            buildList {
                ProblemCatalog.categories.forEach { category ->
                    add(PickerRow.Header(category.emoji, category.title))
                    category.problems.forEach { add(PickerRow.Predefined(it, it.key in keys, overrides[it.key])) }
                }
                val allCustom = (known + custom).distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                if (allCustom.isNotEmpty()) {
                    add(PickerRow.Header("✏️", R.string.category_custom))
                    allCustom.forEach { text ->
                        add(PickerRow.Custom(text, custom.any { it.equals(text, ignoreCase = true) }))
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.find(packageName)?.problems?.let { problems ->
                selectedKeys.value = problems.mapNotNull { it.catalogKey }.toSet()
                selectedCustom.value = problems.mapNotNull { it.customLabel }.toSet()
            }
        }
    }

    fun toggleKey(key: String) = selectedKeys.update { if (key in it) it - key else it + key }

    fun toggleCustom(text: String) = selectedCustom.update { current ->
        val existing = current.firstOrNull { it.equals(text, ignoreCase = true) }
        if (existing != null) current - existing else current + text
    }

    /** Ajoute (et coche) une problématique personnalisée, sans doublon. */
    fun addCustom(text: String) {
        // Réutilise l'orthographe déjà connue pour éviter deux variantes de casse.
        val canonical = knownCustom.value.firstOrNull { it.equals(text, ignoreCase = true) } ?: text
        selectedCustom.update { current ->
            if (current.any { it.equals(canonical, ignoreCase = true) }) current else current + canonical
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.setProblems(packageName, appName, selectedKeys.value, selectedCustom.value)
            onDone()
        }
    }
}
