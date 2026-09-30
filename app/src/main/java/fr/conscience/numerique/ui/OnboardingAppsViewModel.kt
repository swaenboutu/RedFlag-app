package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.container
import fr.conscience.numerique.data.InstalledApp
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.data.matches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Une étape : la problématique [ref] (libellé [label]) est la [position]e sur [total]. */
data class AppsStep(val position: Int, val total: Int, val ref: ProblemRef, val label: String)

sealed interface AppsRow {
    /** Aucune app ne correspond à la recherche. */
    data object Empty : AppsRow

    data class Header(val step: AppsStep) : AppsRow
    data class Choice(val app: InstalledApp, val checked: Boolean, val first: Boolean, val last: Boolean) : AppsRow
}

/**
 * Accueil, étape 3 : pour chaque problématique choisie à l'étape 2, cocher les applications concernées.
 * Le choix n'est enregistré qu'avec « Suivant » ; « Skip » passe à la suite sans rien changer.
 */
class OnboardingAppsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container
    private val repository = container.repository

    private val steps = MutableStateFlow<List<AppsStep>?>(null)
    private val index = MutableStateFlow(0)
    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val _query = MutableStateFlow("")

    /** Texte de recherche ; remis à zéro à chaque changement d'étape. */
    val query: StateFlow<String> = _query
    private var initiallyLinked = emptySet<String>()

    /** Null tant que le chargement n'est pas terminé ; 0 s'il n'y a aucune problématique à traiter. */
    val stepCount: StateFlow<Int?> = steps.combine(index) { steps, _ -> steps?.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rows: StateFlow<List<AppsRow>> = combine(steps, index, apps, selected, _query) { steps, index, apps, selected, query ->
        val step = steps?.getOrNull(index) ?: return@combine emptyList()
        val shown = apps.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
        buildList {
            add(AppsRow.Header(step))
            if (shown.isEmpty() && apps.isNotEmpty()) add(AppsRow.Empty)
            shown.forEachIndexed { i, app ->
                add(AppsRow.Choice(app, app.packageName in selected, i == 0, i == shown.lastIndex))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasSelection: StateFlow<Boolean> = selected.combine(steps) { selected, _ -> selected.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            val favorites = repository.favorites.first()
            val overrides = repository.labelOverrides.first()
            val context = getApplication<Application>()
            val chosen = ProblemCatalog.allRefs.filter { it in favorites }
            steps.value = chosen.mapIndexed { i, ref ->
                AppsStep(i + 1, chosen.size, ref, ref.displayLabel(context, ref.catalogKey?.let(overrides::get)))
            }
            apps.value = withContext(Dispatchers.IO) {
                container.installedApps.list(container.settings.hideSystemApps.value).sortedBy { it.label.lowercase() }
            }
            steps.value?.firstOrNull()?.let { loadSelection(it.ref) }
        }
    }

    /** Reprend ce qui est déjà associé à la problématique, pour ne rien défaire en revenant sur une étape. */
    private suspend fun loadSelection(ref: ProblemRef) {
        initiallyLinked = repository.monitoredApps.first()
            .filter { app -> app.problems.any { it.matches(ref) } }
            .map { it.app.packageName }
            .toSet()
        selected.value = initiallyLinked
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun toggle(packageName: String) = selected.update { if (packageName in it) it - packageName else it + packageName }

    /** Enregistre l'étape courante, puis passe à la suivante ; [onFinished] est appelé après la dernière. */
    fun next(onFinished: () -> Unit) {
        viewModelScope.launch {
            val step = steps.value?.getOrNull(index.value)
            if (step != null) {
                val labels = apps.value.associate { it.packageName to it.label }
                (selected.value - initiallyLinked).forEach { pkg ->
                    repository.linkProblem(pkg, labels[pkg] ?: pkg, step.ref)
                }
                (initiallyLinked - selected.value).forEach { repository.unlinkProblem(it, step.ref) }
            }
            advance(onFinished)
        }
    }

    fun skip(onFinished: () -> Unit) {
        viewModelScope.launch { advance(onFinished) }
    }

    private suspend fun advance(onFinished: () -> Unit) {
        val all = steps.value.orEmpty()
        if (index.value + 1 < all.size) {
            index.value += 1
            _query.value = ""
            loadSelection(all[index.value].ref)
        } else {
            onFinished()
        }
    }

    /** Revient à l'étape précédente ; faux s'il n'y en a pas (l'appelant quitte alors l'écran). */
    fun back(): Boolean {
        if (index.value == 0) return false
        index.value -= 1
        _query.value = ""
        viewModelScope.launch { steps.value?.getOrNull(index.value)?.let { loadSelection(it.ref) } }
        return true
    }
}
