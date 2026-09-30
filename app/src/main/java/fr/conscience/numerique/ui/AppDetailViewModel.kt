package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.container
import fr.conscience.numerique.R
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.toRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Clés des extras de l'intent, lues aussi par le SavedStateHandle du ViewModel. */
object AppDetailArgs {
    const val PACKAGE = "package"
    const val LABEL = "label"
}

sealed interface DetailRow {
    data class Header(val packageName: String, val appName: String, val count: Int) : DetailRow
    data object Section : DetailRow

    /** [id] : ressource du titre, unique par carte (associées, favoris, thèmes). */
    data class Theme(val id: Int, val problemCount: Int, val checkedCount: Int, val expanded: Boolean) : DetailRow

    /** [sectionId] : la carte dans laquelle la ligne s'affiche (une problématique peut figurer dans plusieurs). */
    data class Choice(
        val sectionId: Int,
        val ref: ProblemRef,
        val override: String?,
        val checked: Boolean,
        val first: Boolean,
        val last: Boolean,
    ) : DetailRow

    data class Info(val appName: String, val count: Int) : DetailRow
}

/**
 * Détail d'une application. Chaque case cochée ou décochée est enregistrée aussitôt, comme sur l'écran
 * des problématiques : l'état affiché vient toujours de la base.
 */
class AppDetailViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = application.container.repository
    private val packageName: String = checkNotNull(handle[AppDetailArgs.PACKAGE])
    private val appName: String = handle[AppDetailArgs.LABEL] ?: packageName

    /** Les problématiques associées à l'app s'affichent ouvertes ; les thèmes sont fermés au départ. */
    private val expanded = MutableStateFlow(setOf(R.string.category_linked))

    val rows: StateFlow<List<DetailRow>> = combine(
        repository.monitoredApps,
        repository.customProblems,
        repository.labelOverrides,
        repository.favorites,
        expanded,
    ) { monitored, custom, overrides, favorites, expanded ->
        val current = monitored.firstOrNull { it.app.packageName == packageName }?.problems.orEmpty()
            .map { it.toRef() }.toSet()

        val themes = ProblemCatalog.themeContents(custom)
        val knownLabels = custom.map { it.label }.toSet()
        // Les personnalisées sans thème, plus celles déjà associées à l'app mais absentes de la liste.
        val customRefs = (ProblemCatalog.customWithoutTheme(custom) + current.filter { it.customLabel != null && it.customLabel !in knownLabels })
            .distinct()
        val allRefs = themes.flatMap { it.refs } + customRefs

        fun MutableList<DetailRow>.section(id: Int, refs: List<ProblemRef>) {
            val open = id in expanded
            add(DetailRow.Theme(id, refs.size, refs.count { it in current }, open))
            if (!open) return
            refs.forEachIndexed { i, ref ->
                add(DetailRow.Choice(id, ref, ref.catalogKey?.let(overrides::get), ref in current, i == 0, i == refs.lastIndex))
            }
        }

        buildList {
            add(DetailRow.Header(packageName, appName, current.size))
            add(DetailRow.Section)

            // D'abord ce qui est déjà associé à l'app, puis les favoris, puis les thèmes.
            val linked = allRefs.filter { it in current }
            if (linked.isNotEmpty()) section(R.string.category_linked, linked)
            val favoriteRefs = allRefs.filter { it in favorites }
            if (favoriteRefs.isNotEmpty()) section(R.string.category_favorites, favoriteRefs)
            themes.forEach { section(it.category.title, it.refs) }
            section(R.string.category_custom, customRefs)

            add(DetailRow.Info(appName, current.size))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSection(id: Int) = expanded.update { if (id in it) it - id else it + id }

    /** Enregistre aussitôt : [checked] est l'état actuel de la case, on la bascule. */
    fun toggle(ref: ProblemRef, checked: Boolean) {
        viewModelScope.launch {
            if (checked) repository.unlinkProblem(packageName, ref) else repository.linkProblem(packageName, appName, ref)
        }
    }
}
