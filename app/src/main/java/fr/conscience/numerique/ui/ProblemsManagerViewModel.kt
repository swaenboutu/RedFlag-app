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
        @StringRes val id: Int,
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
        val label: String,
        val apps: List<LinkedApp>,
        val first: Boolean,
        val last: Boolean,
    ) : ManagerRow
}

/** Référence de la problématique portée par une ligne (null pour un thème). */
fun ManagerRow.toRef(): ProblemRef? = when (this) {
    is ManagerRow.Catalog -> ProblemRef(catalogKey = problem.key)
    is ManagerRow.Custom -> ProblemRef(customLabel = label)
    is ManagerRow.Theme -> null
}

private sealed interface Entry {
    val apps: List<LinkedApp>

    data class Cat(val problem: PredefinedProblem, override val apps: List<LinkedApp>) : Entry
    data class Cus(val label: String, override val apps: List<LinkedApp>) : Entry
}

class ProblemsManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConscienceApp).container.repository

    /** Les favoris s'affichent ouverts la première fois ; l'utilisateur peut les refermer. */
    private val expanded = MutableStateFlow(setOf(R.string.category_favorites))

    val rows: StateFlow<List<ManagerRow>> = combine(
        repository.monitoredApps,
        repository.customLabels,
        repository.labelOverrides,
        repository.favorites,
        expanded,
    ) { monitored, custom, overrides, favorites, expanded ->
        val appsByProblem = HashMap<ProblemRef, MutableList<LinkedApp>>()
        monitored.forEach { entry ->
            val app = LinkedApp(entry.app.packageName, entry.app.appName)
            entry.problems.forEach { p ->
                appsByProblem.getOrPut(ProblemRef(p.catalogKey, p.customLabel)) { mutableListOf() }.add(app)
            }
        }
        fun appsOf(ref: ProblemRef): List<LinkedApp> = appsByProblem[ref].orEmpty().sortedBy { it.appName.lowercase() }

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
                        is Entry.Cus -> ManagerRow.Custom(sectionId, entry.label, entry.apps, first, last)
                    },
                )
            }
        }

        val catalogEntries = ProblemCatalog.categories.map { category ->
            category.title to category.problems.map { Entry.Cat(it, appsOf(ProblemRef(catalogKey = it.key))) }
        }
        val customEntries = custom.map { Entry.Cus(it, appsOf(ProblemRef(customLabel = it))) }

        buildList {
            // Favoris : d'abord ceux du catalogue (dans l'ordre des thèmes), puis les personnalisés.
            val favoriteEntries: List<Entry> =
                catalogEntries.flatMap { it.second }.filter { ProblemRef(catalogKey = it.problem.key) in favorites } +
                    customEntries.filter { ProblemRef(customLabel = it.label) in favorites }
            if (favoriteEntries.isNotEmpty()) addSection(R.string.category_favorites, favoriteEntries)

            catalogEntries.forEach { (title, entries) -> addSection(title, entries) }
            addSection(R.string.category_custom, customEntries)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(themeId: Int) = expanded.update { if (themeId in it) it - themeId else it + themeId }

    fun expandCustom() = expanded.update { it + R.string.category_custom }

    /** [onResult] reçoit false si le nom est déjà pris. */
    fun add(label: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.addCustomProblem(label)) }
    }
}
