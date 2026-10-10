package app.redflag.ui.onboarding

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.redflag.AppContainer
import app.redflag.R
import app.redflag.data.InstalledApp
import app.redflag.data.ProblemCatalog
import app.redflag.data.ProblemRef
import app.redflag.data.displayDescription
import app.redflag.data.displayLabel
import app.redflag.data.matches
import app.redflag.util.alphabetical
import app.redflag.util.matchesSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Une étape : la problématique [ref] (libellé [label]) est la [position]e sur [total]. [themeTitle] : titre de son thème ;
 * [body] : son explication (si elle en a une), suivie de la consigne.
 */
data class AppsStep(
    val position: Int,
    val total: Int,
    val ref: ProblemRef,
    val label: String,
    @param:StringRes val themeTitle: Int,
    val body: String,
)

/**
 * Le bas de l'écran : [finishLater] (dernière page alors que d'autres enjeux ont été choisis) remplace « Suivant » par « Terminer plus tard » ;
 * [chosenTotal] enjeux choisis, dont [remaining] ne sont pas traités ici ; [selectedCount] apps cochées ; [canSave] : « Suivant » est actif.
 */
data class AppsBottom(val finishLater: Boolean, val selectedCount: Int, val canSave: Boolean, val chosenTotal: Int, val remaining: Int)

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
    private val chosenTotal = MutableStateFlow(0)

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

    /** What the bottom of the screen shows (see [AppsBottom]). */
    val bottom: StateFlow<AppsBottom> = combine(steps, index, selected, canSave, chosenTotal) { steps, index, selected, canSave, total ->
        val handled = steps?.size ?: 0
        val last = handled > 0 && index == handled - 1
        AppsBottom(
            finishLater = last && total > handled,
            selectedCount = selected.size,
            canSave = canSave,
            chosenTotal = total,
            remaining = (total - handled).coerceAtLeast(0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsBottom(false, 0, false, 0, 0))

    init {
        viewModelScope.launch {
            val favorites = repository.favorites.first()
            val overrides = repository.labelOverrides.first()
            val chosen = ProblemCatalog.allRefs.filter { it in favorites }
            chosenTotal.value = chosen.size
            // However many issues were chosen, the tour handles the first few: the others wait in the Issues tab.
            val handled = chosen.take(MAX_ISSUES)
            val instruction = context.getString(R.string.onboarding_apps_body)
            steps.value = handled.mapIndexed { i, ref ->
                val category = ProblemCatalog.categories.first { c -> c.problems.any { it.key == ref.catalogKey } }
                AppsStep(i + 1, handled.size, ref, ref.displayLabel(context, overrides[ref.id]), category.title, bodyOf(ref.displayDescription(context), instruction))
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

    private fun bodyOf(description: String?, instruction: String): String {
        if (description.isNullOrBlank()) return instruction
        val lead = if (description.last() in ".…") description else "$description."
        return "$lead $instruction"
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

/** The welcome tour handles at most this many issues (the first chosen ones). */
const val MAX_ISSUES = 3
