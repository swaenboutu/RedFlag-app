package fr.conscience.numerique.data

import android.content.Context
import androidx.annotation.StringRes
import fr.conscience.numerique.R

/** Problématique prédéfinie : [key] est stable et stockée en base, [label] est traduit par les ressources. */
data class PredefinedProblem(val key: String, @StringRes val label: Int)

data class ProblemCategory(
    val emoji: String,
    @StringRes val title: Int,
    val problems: List<PredefinedProblem>,
)

object ProblemCatalog {
    val categories: List<ProblemCategory> = listOf(
        ProblemCategory(
            "🧠", R.string.category_mental,
            listOf(
                PredefinedProblem("addictive_design", R.string.problem_addictive_design),
                PredefinedProblem("social_comparison", R.string.problem_social_comparison),
                PredefinedProblem("fomo", R.string.problem_fomo),
                PredefinedProblem("sleep_impact", R.string.problem_sleep_impact),
            ),
        ),
        ProblemCategory(
            "👥", R.string.category_social,
            listOf(
                PredefinedProblem("sexism", R.string.problem_sexism),
                PredefinedProblem("racism", R.string.problem_racism),
                PredefinedProblem("harassment", R.string.problem_harassment),
                PredefinedProblem("hate_content", R.string.problem_hate_content),
            ),
        ),
        ProblemCategory(
            "🌍", R.string.category_environment,
            listOf(
                PredefinedProblem("carbon_footprint", R.string.problem_carbon_footprint),
                PredefinedProblem("planned_obsolescence", R.string.problem_planned_obsolescence),
                PredefinedProblem("overconsumption", R.string.problem_overconsumption),
            ),
        ),
        ProblemCategory(
            "💼", R.string.category_exploitation,
            listOf(
                PredefinedProblem("worker_exploitation", R.string.problem_worker_exploitation),
                PredefinedProblem("child_labor", R.string.problem_child_labor),
                PredefinedProblem("subcontractor_conditions", R.string.problem_subcontractor_conditions),
            ),
        ),
        ProblemCategory(
            "🔐", R.string.category_privacy,
            listOf(
                PredefinedProblem("excessive_data", R.string.problem_excessive_data),
                PredefinedProblem("data_resale", R.string.problem_data_resale),
                PredefinedProblem("surveillance", R.string.problem_surveillance),
            ),
        ),
        ProblemCategory(
            "💰", R.string.category_economic,
            listOf(
                PredefinedProblem("predatory_monetization", R.string.problem_predatory_monetization),
                PredefinedProblem("monopoly", R.string.problem_monopoly),
                PredefinedProblem("tax_evasion", R.string.problem_tax_evasion),
            ),
        ),
        ProblemCategory(
            "🏛️", R.string.category_political,
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
}

/** Intitulé d'une entrée du catalogue : celui choisi par l'utilisateur, sinon la traduction courante. */
fun PredefinedProblem.displayLabel(context: Context, overrides: Map<String, String>): String =
    overrides[key] ?: context.getString(label)

/** Texte à afficher : catalogue (surcharge ou traduction) ou texte libre tel quel. */
fun Problem.displayLabel(context: Context, overrides: Map<String, String>): String? =
    catalogKey?.let { key -> ProblemCatalog.find(key)?.displayLabel(context, overrides) } ?: customLabel
