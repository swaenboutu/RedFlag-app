package app.redflag.data

import android.content.Context
import androidx.annotation.StringRes
import app.redflag.R

/** Problématique prédéfinie : [key] est stable et stockée en base, [label] est traduit par les ressources. */
data class PredefinedProblem(val key: String, @StringRes val label: Int)

data class ProblemCategory(
    /** Stable, stockée en base pour rattacher une problématique personnalisée à ce thème. */
    val key: String,
    val emoji: String,
    @StringRes val title: Int,
    val problems: List<PredefinedProblem>,
) {
    /** Les problématiques du catalogue de ce thème, sous forme de références. */
    val refs: List<ProblemRef> get() = problems.map { ProblemRef.catalog(it.key) }
}

/** Le contenu d'une carte de thème : ses problématiques du catalogue, puis les personnalisées qui y sont rangées. */
data class ThemeContent(val category: ProblemCategory, val refs: List<ProblemRef>)

object ProblemCatalog {
    val categories: List<ProblemCategory> = listOf(
        ProblemCategory(
            "mental", "🧠", R.string.category_mental,
            listOf(
                PredefinedProblem("addictive_design", R.string.problem_addictive_design),
                PredefinedProblem("social_comparison", R.string.problem_social_comparison),
                PredefinedProblem("fomo", R.string.problem_fomo),
                PredefinedProblem("sleep_impact", R.string.problem_sleep_impact),
            ),
        ),
        ProblemCategory(
            "social", "👥", R.string.category_social,
            listOf(
                PredefinedProblem("sexism", R.string.problem_sexism),
                PredefinedProblem("racism", R.string.problem_racism),
                PredefinedProblem("harassment", R.string.problem_harassment),
                PredefinedProblem("hate_content", R.string.problem_hate_content),
            ),
        ),
        ProblemCategory(
            "environment", "🌍", R.string.category_environment,
            listOf(
                PredefinedProblem("carbon_footprint", R.string.problem_carbon_footprint),
                PredefinedProblem("planned_obsolescence", R.string.problem_planned_obsolescence),
                PredefinedProblem("overconsumption", R.string.problem_overconsumption),
            ),
        ),
        ProblemCategory(
            "exploitation", "💼", R.string.category_exploitation,
            listOf(
                PredefinedProblem("worker_exploitation", R.string.problem_worker_exploitation),
                PredefinedProblem("child_labor", R.string.problem_child_labor),
                PredefinedProblem("subcontractor_conditions", R.string.problem_subcontractor_conditions),
            ),
        ),
        ProblemCategory(
            "privacy", "🔐", R.string.category_privacy,
            listOf(
                PredefinedProblem("excessive_data", R.string.problem_excessive_data),
                PredefinedProblem("data_resale", R.string.problem_data_resale),
                PredefinedProblem("surveillance", R.string.problem_surveillance),
            ),
        ),
        ProblemCategory(
            "economic", "💰", R.string.category_economic,
            listOf(
                PredefinedProblem("predatory_monetization", R.string.problem_predatory_monetization),
                PredefinedProblem("monopoly", R.string.problem_monopoly),
                PredefinedProblem("tax_evasion", R.string.problem_tax_evasion),
            ),
        ),
        ProblemCategory(
            "political", "🏛️", R.string.category_political,
            listOf(
                PredefinedProblem("misinformation", R.string.problem_misinformation),
                PredefinedProblem("polarization", R.string.problem_polarization),
                PredefinedProblem("authoritarian_funding", R.string.problem_authoritarian_funding),
            ),
        ),
    )

    private val byKey: Map<String, PredefinedProblem> =
        categories.flatMap { it.problems }.associateBy { it.key }

    fun find(key: String): PredefinedProblem? = byKey[key]

    fun findCategory(key: String): ProblemCategory? = categories.firstOrNull { it.key == key }

    /** Toutes les problématiques du catalogue, dans l'ordre des thèmes. */
    val allRefs: List<ProblemRef> = categories.flatMap { it.refs }

    /** Les thèmes avec, à la suite du catalogue, les problématiques personnalisées [custom] rangées dedans. */
    fun themeContents(custom: List<CustomProblem>): List<ThemeContent> {
        val byCategory = custom.groupBy({ it.category }, { ProblemRef(it.id) })
        return categories.map { ThemeContent(it, it.refs + byCategory[it.key].orEmpty()) }
    }

    /** Les problématiques personnalisées sans thème (ou dont le thème n'existe plus) : elles vont dans « Personnalisé ». */
    fun customWithoutTheme(custom: List<CustomProblem>): List<ProblemRef> =
        custom.filter { it.category?.let(::findCategory) == null }.map { ProblemRef(it.id) }
}

/** Intitulé d'une entrée du catalogue : [override] (choisi par l'utilisateur), sinon la traduction courante. */
fun PredefinedProblem.displayLabel(context: Context, override: String?): String =
    override ?: context.getString(label)

fun PredefinedProblem.displayLabel(context: Context, overrides: Map<String, String>): String =
    displayLabel(context, overrides[key])

/** Texte à afficher pour une référence : catalogue ([override] ou traduction) ; une personnalisée n'a que son nom, passé en [override]. */
fun ProblemRef.displayLabel(context: Context, override: String?): String =
    catalogKey?.let(ProblemCatalog::find)?.displayLabel(context, override) ?: override.orEmpty()

/** Texte à afficher : catalogue (surcharge ou traduction) ou nom de la personnalisée. [overrides] : voir `AppRepository.labelOverrides`. */
fun Problem.displayLabel(context: Context, overrides: Map<String, String>): String? =
    ProblemCatalog.find(problemId)?.displayLabel(context, overrides) ?: overrides[problemId]
