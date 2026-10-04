package fr.conscience.numerique.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.AppContainer
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OnboardingRow {
    data object Header : OnboardingRow

    /** [id] : ressource du titre du thème. */
    data class Theme(val id: Int, val problemCount: Int, val checkedCount: Int, val expanded: Boolean) : OnboardingRow

    data class Choice(
        val sectionId: Int,
        val ref: ProblemRef,
        val override: String?,
        val checked: Boolean,
        val first: Boolean,
        val last: Boolean,
    ) : OnboardingRow
}

/**
 * Accueil, étape 2 : l'utilisateur coche les problématiques qui comptent pour lui, sans limite (il peut passer l'association aux apps). Elles deviennent ses favoris,
 * enregistrés aussitôt (l'état affiché vient toujours de la base). Les thèmes sont fermés au départ.
 */
class OnboardingProblemsViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val expanded = MutableStateFlow(emptySet<Int>())

    /** Nombre de problématiques du catalogue déjà cochées (les seules visibles sur cet écran). */
    val selectedCount: StateFlow<Int> = repository.favorites
        .map { favorites -> ProblemCatalog.allRefs.count { it in favorites } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val rows: StateFlow<List<OnboardingRow>> = combine(
        repository.favorites,
        repository.labelOverrides,
        expanded,
    ) { favorites, overrides, expanded ->
        buildList {
            add(OnboardingRow.Header)
            ProblemCatalog.categories.forEach { category ->
                val refs = category.refs
                val open = category.title in expanded
                add(OnboardingRow.Theme(category.title, refs.size, refs.count { it in favorites }, open))
                if (!open) return@forEach
                refs.forEachIndexed { i, ref ->
                    add(OnboardingRow.Choice(category.title, ref, overrides[ref.id], ref in favorites, i == 0, i == refs.lastIndex))
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSection(id: Int) = expanded.update { if (id in it) it - id else it + id }

    /** [checked] est l'état actuel de la case : on la bascule. */
    fun toggle(ref: ProblemRef, checked: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(ref, !checked)
        }
    }
}
