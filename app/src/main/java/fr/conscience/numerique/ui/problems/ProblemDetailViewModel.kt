package fr.conscience.numerique.ui.problems

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.AppContainer
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.matches
import fr.conscience.numerique.ui.common.displayedLabels
import fr.conscience.numerique.ui.common.isLabelTaken
import fr.conscience.numerique.util.alphabetical
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Clés des extras de l'intent, lues aussi par le SavedStateHandle du ViewModel. */
object DetailArgs {
    const val PROBLEM_ID = "problemId"
}

data class LinkedApp(val packageName: String, val appName: String)

data class DetailState(
    val ref: ProblemRef,
    /** Intitulé choisi par l'utilisateur pour une problématique du catalogue, s'il y en a un. */
    val override: String?,
    val linkedApps: List<LinkedApp>,
    val favorite: Boolean,
)

class ProblemDetailViewModel(container: AppContainer, private val context: Context, private val handle: SavedStateHandle) :
    ViewModel() {
    private val repository = container.repository
    private val ref = ProblemRef(checkNotNull(handle[DetailArgs.PROBLEM_ID]))

    val state: StateFlow<DetailState?> =
        combine(repository.monitoredApps, repository.labelOverrides, repository.favorites) { apps, overrides, favorites ->
            val linked = apps
                .filter { app -> app.problems.any { it.matches(ref) } }
                .map { LinkedApp(it.app.packageName, it.app.appName) }
                .sortedWith(compareBy(alphabetical()) { it.appName })
            DetailState(ref, overrides[ref.id], linked, ref in favorites)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Renomme n'importe quelle problématique ; [onResult] reçoit false si le nom est déjà celui d'une autre problématique
     * (du catalogue ou personnalisée). [originalLabel] : l'intitulé d'origine d'une problématique du catalogue ; s'il est
     * retapé tel quel, on rétablit simplement l'original (traduit) au lieu d'enregistrer un texte figé.
     */
    fun rename(newLabel: String, originalLabel: String? = null, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (repository.displayedLabels(context).isLabelTaken(newLabel, except = ref)) {
                onResult(false)
                return@launch
            }
            val catalogKey = ref.catalogKey
            if (catalogKey != null) {
                if (originalLabel != null && newLabel == originalLabel) {
                    repository.resetCatalogLabel(catalogKey)
                } else {
                    repository.setCatalogLabel(catalogKey, newLabel)
                }
                onResult(true)
            } else {
                onResult(repository.renameCustomProblem(ref.id, newLabel))
            }
        }
    }

    fun toggleFavorite() {
        val current = state.value ?: return
        viewModelScope.launch { repository.setFavorite(current.ref, !current.favorite) }
    }

    /** Rétablit l'intitulé d'origine (traduit) d'une problématique du catalogue. */
    fun resetLabel() {
        val catalogKey = ref.catalogKey ?: return
        viewModelScope.launch { repository.resetCatalogLabel(catalogKey) }
    }

    fun link(apps: List<LinkedApp>) {
        viewModelScope.launch { apps.forEach { repository.linkProblem(it.packageName, it.appName, ref) } }
    }

    fun unlink(packageName: String) {
        viewModelScope.launch { repository.unlinkProblem(packageName, ref) }
    }

    /** Supprime une problématique personnalisée de toutes les apps et de la liste. */
    fun deleteCustom(onDone: () -> Unit) {
        if (!ref.isCustom) return
        viewModelScope.launch {
            repository.deleteCustomProblem(ref.id)
            onDone()
        }
    }
}
