package fr.conscience.numerique.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.ConscienceApp
import fr.conscience.numerique.R
import fr.conscience.numerique.data.PredefinedProblem
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ManagerRow {
    data class Header(val emoji: String, @StringRes val title: Int) : ManagerRow
    data class Catalog(val problem: PredefinedProblem, val apps: Int, val override: String?) : ManagerRow
    data class Custom(val label: String, val apps: Int) : ManagerRow
}

/** Référence de la problématique portée par une ligne (null pour un en-tête). */
fun ManagerRow.toRef(): ProblemRef? = when (this) {
    is ManagerRow.Catalog -> ProblemRef(catalogKey = problem.key)
    is ManagerRow.Custom -> ProblemRef(customLabel = label)
    is ManagerRow.Header -> null
}

class ProblemsManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConscienceApp).container.repository

    val rows: StateFlow<List<ManagerRow>> =
        combine(repository.problems, repository.customLabels, repository.labelOverrides) { problems, custom, overrides ->
            val byKey = problems.mapNotNull { it.catalogKey }.groupingBy { it }.eachCount()
            val byLabel = problems.mapNotNull { it.customLabel }.groupingBy { it }.eachCount()
            buildList {
                ProblemCatalog.categories.forEach { category ->
                    add(ManagerRow.Header(category.emoji, category.title))
                    category.problems.forEach {
                        add(ManagerRow.Catalog(it, byKey[it.key] ?: 0, overrides[it.key]))
                    }
                }
                add(ManagerRow.Header("✏️", R.string.category_custom))
                custom.forEach { add(ManagerRow.Custom(it, byLabel[it] ?: 0)) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [onResult] reçoit false si le nom est déjà pris. */
    fun add(label: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.addCustomProblem(label)) }
    }
}
