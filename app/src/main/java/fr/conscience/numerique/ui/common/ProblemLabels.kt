package fr.conscience.numerique.ui.common

import android.content.Context
import fr.conscience.numerique.data.AppRepository
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.displayLabel
import kotlinx.coroutines.flow.first

/** Tous les intitulés de problématiques affichés en ce moment : catalogue (renommages compris), puis personnalisées. */
suspend fun AppRepository.displayedLabels(context: Context): Map<ProblemRef, String> {
    val overrides = labelOverrides.first()
    val catalog = ProblemCatalog.categories.flatMap { it.problems }
        .associate { ProblemRef.catalog(it.key) to it.displayLabel(context, overrides) }
    val custom = customProblems.first().associate { ProblemRef(it.id) to it.label }
    return catalog + custom
}

/**
 * Vrai si [label] est déjà l'intitulé d'une autre problématique (casse ignorée). [except] : la problématique qu'on est en
 * train de renommer, qui peut garder son propre nom (ou n'en changer que la casse).
 */
fun Map<ProblemRef, String>.isLabelTaken(label: String, except: ProblemRef? = null): Boolean =
    any { (ref, text) -> ref != except && text.equals(label, ignoreCase = true) }
