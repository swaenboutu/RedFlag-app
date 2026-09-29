package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.ConscienceApp
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.matches
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Clés des extras de l'intent, lues aussi par le SavedStateHandle du ViewModel. */
object DetailArgs {
    const val CATALOG_KEY = "catalogKey"
    const val CUSTOM_LABEL = "customLabel"
}

data class LinkedApp(val packageName: String, val appName: String)

data class DetailState(
    val ref: ProblemRef,
    /** Intitulé choisi par l'utilisateur pour une problématique du catalogue, s'il y en a un. */
    val override: String?,
    val linkedApps: List<LinkedApp>,
    val favorite: Boolean,
)

class ProblemDetailViewModel(application: Application, private val handle: SavedStateHandle) :
    AndroidViewModel(application) {
    private val repository = (application as ConscienceApp).container.repository
    private val catalogKey: String? = handle[DetailArgs.CATALOG_KEY]

    /** Le texte libre est dans le SavedStateHandle : il change quand on renomme. */
    private val ref = handle.getStateFlow<String?>(DetailArgs.CUSTOM_LABEL, null)
        .map { ProblemRef(catalogKey, it) }

    val state: StateFlow<DetailState?> =
        combine(ref, repository.monitoredApps, repository.labelOverrides, repository.favorites) { ref, apps, overrides, favorites ->
            val linked = apps
                .filter { app -> app.problems.any { it.matches(ref) } }
                .map { LinkedApp(it.app.packageName, it.app.appName) }
                .sortedBy { it.appName.lowercase() }
            DetailState(ref, ref.catalogKey?.let(overrides::get), linked, ref in favorites)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Renomme n'importe quelle problématique ; [onResult] reçoit false si le nom est déjà pris. */
    fun rename(newLabel: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val current = handle.get<String?>(DetailArgs.CUSTOM_LABEL)
            if (catalogKey != null) {
                repository.setCatalogLabel(catalogKey, newLabel)
                onResult(true)
            } else if (current != null && repository.renameCustomProblem(current, newLabel)) {
                handle[DetailArgs.CUSTOM_LABEL] = newLabel
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun toggleFavorite() {
        val current = state.value ?: return
        viewModelScope.launch { repository.setFavorite(current.ref, !current.favorite) }
    }

    /** Rétablit l'intitulé d'origine (traduit) d'une problématique du catalogue. */
    fun resetLabel() {
        catalogKey ?: return
        viewModelScope.launch { repository.resetCatalogLabel(catalogKey) }
    }

    fun link(apps: List<LinkedApp>) {
        val target = ProblemRef(catalogKey, handle[DetailArgs.CUSTOM_LABEL])
        viewModelScope.launch { apps.forEach { repository.linkProblem(it.packageName, it.appName, target) } }
    }

    fun unlink(packageName: String) {
        val target = ProblemRef(catalogKey, handle[DetailArgs.CUSTOM_LABEL])
        viewModelScope.launch { repository.unlinkProblem(packageName, target) }
    }

    /** Supprime une problématique personnalisée de toutes les apps et de la liste. */
    fun deleteCustom(onDone: () -> Unit) {
        val label = handle.get<String?>(DetailArgs.CUSTOM_LABEL) ?: return
        viewModelScope.launch {
            repository.deleteCustomProblem(label)
            onDone()
        }
    }
}
