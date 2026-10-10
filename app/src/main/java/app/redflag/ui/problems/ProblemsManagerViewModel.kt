package app.redflag.ui.problems

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.redflag.AppContainer
import app.redflag.R
import app.redflag.data.PredefinedProblem
import app.redflag.data.ProblemCatalog
import app.redflag.data.ProblemRef
import app.redflag.data.toRef
import app.redflag.ui.common.displayedLabels
import app.redflag.ui.common.isLabelTaken
import app.redflag.util.alphabetical
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Une ligne de l'écran « Vos problématiques » : un thème (carte repliable) ou une problématique de ce thème. */
sealed interface ManagerRow {
    /** [id] est la ressource du titre : unique par thème (favoris compris). */
    data class Theme(
        @param:StringRes val id: Int,
        val problemCount: Int,
        val appCount: Int,
        val expanded: Boolean,
    ) : ManagerRow

    /** [sectionId] : le thème ou « Vos favoris » dans lequel la ligne s'affiche (une problématique peut figurer dans les deux). */
    data class Catalog(
        val sectionId: Int,
        val problem: PredefinedProblem,
        val override: String?,
        val apps: List<LinkedApp>,
        val first: Boolean,
        val last: Boolean,
    ) : ManagerRow

    data class Custom(
        val sectionId: Int,
        val id: String,
        val label: String,
        val apps: List<LinkedApp>,
        val first: Boolean,
        val last: Boolean,
    ) : ManagerRow
}

/** Référence de la problématique portée par une ligne (null pour un thème). */
fun ManagerRow.toRef(): ProblemRef? = when (this) {
    is ManagerRow.Catalog -> ProblemRef.catalog(problem.key)
    is ManagerRow.Custom -> ProblemRef(id)
    is ManagerRow.Theme -> null
}

private sealed interface Entry {
    val apps: List<LinkedApp>

    data class Cat(val problem: PredefinedProblem, override val apps: List<LinkedApp>) : Entry
    data class Cus(val id: String, val label: String, override val apps: List<LinkedApp>) : Entry
}

class ProblemsManagerViewModel(container: AppContainer, private val context: Context) : ViewModel() {
    private val repository = container.repository

    /** Les favoris s'affichent ouverts la première fois ; l'utilisateur peut les refermer. */
    private val expanded = MutableStateFlow(setOf(R.string.category_favorites))

    val rows: StateFlow<List<ManagerRow>> = combine(
        repository.monitoredApps,
        repository.customProblems,
        repository.labelOverrides,
        repository.favorites,
        expanded,
    ) { monitored, custom, overrides, favorites, expanded ->
        val appsByProblem = HashMap<ProblemRef, MutableList<LinkedApp>>()
        monitored.forEach { entry ->
            val app = LinkedApp(entry.app.packageName, entry.app.appName)
            entry.problems.forEach { p ->
                appsByProblem.getOrPut(p.toRef()) { mutableListOf() }.add(app)
            }
        }
        fun appsOf(ref: ProblemRef): List<LinkedApp> = appsByProblem[ref].orEmpty().sortedWith(compareBy(alphabetical()) { it.appName })

        fun MutableList<ManagerRow>.addSection(sectionId: Int, entries: List<Entry>) {
            val open = sectionId in expanded
            add(ManagerRow.Theme(sectionId, entries.size, entries.flatMap { it.apps }.distinctBy { it.packageName }.size, open))
            if (!open) return
            entries.forEachIndexed { i, entry ->
                val first = i == 0
                val last = i == entries.lastIndex
                add(
                    when (entry) {
                        is Entry.Cat -> ManagerRow.Catalog(sectionId, entry.problem, overrides[entry.problem.key], entry.apps, first, last)
                        is Entry.Cus -> ManagerRow.Custom(sectionId, entry.id, entry.label, entry.apps, first, last)
                    },
                )
            }
        }

        fun entryOf(ref: ProblemRef): Entry {
            val predefined = ref.catalogKey?.let(ProblemCatalog::find)
            return if (predefined != null) Entry.Cat(predefined, appsOf(ref)) else Entry.Cus(ref.id, overrides[ref.id].orEmpty(), appsOf(ref))
        }
        val catalogEntries = ProblemCatalog.themeContents(custom).map { it.category.title to it.refs.map(::entryOf) }
        val customEntries = custom.map { entryOf(ProblemRef(it.id)) as Entry.Cus }

        buildList {
            // Favoris : d'abord ceux du catalogue (dans l'ordre des thèmes), puis les personnalisés.
            val favoriteEntries: List<Entry> =
                catalogEntries.flatMap { it.second }.filterIsInstance<Entry.Cat>().filter { ProblemRef.catalog(it.problem.key) in favorites } +
                    customEntries.filter { ProblemRef(it.id) in favorites }
            if (favoriteEntries.isNotEmpty()) addSection(R.string.category_favorites, favoriteEntries)

            catalogEntries.forEach { (title, entries) -> addSection(title, entries) }
            addSection(R.string.category_custom, ProblemCatalog.customWithoutTheme(custom).map(::entryOf))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(themeId: Int) = expanded.update { if (themeId in it) it - themeId else it + themeId }

    /** Ouvre le thème [categoryKey] (null = « Personnalisé »), par exemple après y avoir ajouté une problématique. */
    fun expandTheme(categoryKey: String?) = expanded.update {
        it + (categoryKey?.let { key -> ProblemCatalog.findCategory(key)?.title } ?: R.string.category_custom)
    }

    /** [onResult] reçoit false si le nom est déjà pris. */
    fun add(label: String, category: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Refusé si une autre problématique, du catalogue ou personnalisée, porte déjà ce nom.
            if (repository.displayedLabels(context).isLabelTaken(label)) {
                onResult(false)
                return@launch
            }
            onResult(repository.addCustomProblem(label, category) != null)
        }
    }
}
