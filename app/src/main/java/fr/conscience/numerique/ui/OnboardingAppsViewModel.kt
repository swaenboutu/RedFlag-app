package fr.conscience.numerique.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.AppContainer
import fr.conscience.numerique.util.matchesSearch
import fr.conscience.numerique.util.alphabetical
import fr.conscience.numerique.data.InstalledApp
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.data.matches
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
class OnboardingAppsViewModel(private val container: AppContainer, private val context: Context) : ViewModel() {
    private val repository = container.repository

    private val steps = MutableStateFlow<List<AppsStep>?>(null)
    private val index = MutableStateFlow(0)
    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val _query = MutableStateFlow("")

    /** Texte de recherche ; remis à zéro à chaque changement d'étape. */
    val query: StateFlow<String> = _query

    /** Apps déjà associées à la problématique affichée, telles qu'enregistrées : ce que « Suivant » modifie. */
    private val initiallyLinked = MutableStateFlow<Set<String>>(emptySet())

    /** Null tant que le chargement n'est pas terminé ; 0 s'il n'y a aucune problématique à traiter. */
    val stepCount: StateFlow<Int?> = steps.combine(index) { steps, _ -> steps?.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rows: StateFlow<List<AppsRow>> = combine(steps, index, apps, selected, _query) { steps, index, apps, selected, query ->
        val step = steps?.getOrNull(index) ?: return@combine emptyList()
        val shown = apps.filter { it.label.matchesSearch(query) }
        buildList {
            add(AppsRow.Header(step))
            if (shown.isEmpty() && apps.isNotEmpty()) add(AppsRow.Empty)
            shown.forEachIndexed { i, app ->
                add(AppsRow.Choice(app, app.packageName in selected, i == 0, i == shown.lastIndex))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * « Suivant » a quelque chose à enregistrer : des apps cochées, ou des apps déjà associées qu'on vient de toutes décocher
     * (sans cela, il serait impossible de retirer la dernière).
     */
    val canSave: StateFlow<Boolean> = combine(selected, initiallyLinked) { selected, initial ->
        selected.isNotEmpty() || initial.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            val favorites = repository.favorites.first()
            val overrides = repository.labelOverrides.first()
            val chosen = ProblemCatalog.allRefs.filter { it in favorites }
            steps.value = chosen.mapIndexed { i, ref ->
                AppsStep(i + 1, chosen.size, ref, ref.displayLabel(context, overrides[ref.id]))
            }
            // La sélection de départ est chargée avant d'afficher les apps : un clic ne peut pas être écrasé par ce chargement.
            steps.value?.firstOrNull()?.let { loadSelection(it.ref) }
            apps.value = container.installedAppsFlow().first().sortedWith(compareBy(alphabetical()) { it.label })
        }
    }

    /** Reprend ce qui est déjà associé à la problématique, pour ne rien défaire en revenant sur une étape. */
    private suspend fun loadSelection(ref: ProblemRef) {
        val linked = repository.monitoredApps.first()
            .filter { app -> app.problems.any { it.matches(ref) } }
            .map { it.app.packageName }
            .toSet()
        initiallyLinked.value = linked
        selected.value = linked
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
                val before = initiallyLinked.value
                (selected.value - before).forEach { pkg ->
                    repository.linkProblem(pkg, labels[pkg] ?: pkg, step.ref)
                }
                (before - selected.value).forEach { repository.unlinkProblem(it, step.ref) }
                // Ce qui vient d'être enregistré devient la référence : sur la dernière étape, l'utilisateur peut revenir
                // en arrière et décocher, il faut alors comparer à ce choix et non à l'état d'avant.
                initiallyLinked.value = selected.value
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
