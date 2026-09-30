package fr.conscience.numerique.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    /** Timestamp (ms) jusqu'auquel la friction est suspendue pour cette app ; 0 = jamais suspendue. */
    val snoozedUntil: Long = 0,
)

/**
 * Une problématique associée à une app : soit une entrée du catalogue prédéfini ([catalogKey],
 * traduite à l'affichage), soit un texte libre ([customLabel], affiché tel quel). Exactement un des
 * deux est renseigné.
 */
@Entity(
    tableName = "problems",
    foreignKeys = [
        ForeignKey(
            entity = MonitoredApp::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packageName")],
)
data class Problem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val catalogKey: String? = null,
    val customLabel: String? = null,
)

/**
 * Liste des problématiques personnalisées connues, indépendamment des apps auxquelles elles sont associées.
 * [category] : clé du thème du catalogue où la ranger ; null = thème « Personnalisé ».
 */
@Entity(tableName = "custom_problems")
data class CustomProblem(@PrimaryKey val label: String, val category: String? = null)

/** Intitulé choisi par l'utilisateur pour une problématique du catalogue ; remplace la traduction, dans toutes les langues. */
@Entity(tableName = "catalog_overrides")
data class CatalogOverride(@PrimaryKey val catalogKey: String, val label: String)

/** Problématique mise en favori ; [id] encode la référence : « catalog:clé » ou « custom:texte ». */
@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val id: String)

@Entity(tableName = "choice_events", indices = [Index("packageName")])
data class ChoiceEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    /** true = « Oui, continuer », false = « Non, revenir en arrière ». */
    val proceeded: Boolean,
)

data class MonitoredAppWithProblems(
    @Embedded val app: MonitoredApp,
    @Relation(parentColumn = "packageName", entityColumn = "packageName")
    val problems: List<Problem>,
)

data class RefusalCount(val packageName: String, val refusals: Int)

/** Identifie une problématique : une entrée du catalogue ([catalogKey]) ou un texte libre ([customLabel]). */
data class ProblemRef(val catalogKey: String? = null, val customLabel: String? = null)

fun Problem.toRef() = ProblemRef(catalogKey, customLabel)

fun Problem.matches(ref: ProblemRef) = catalogKey == ref.catalogKey && customLabel == ref.customLabel

private const val CATALOG_PREFIX = "catalog:"
const val CUSTOM_PREFIX = "custom:"

fun ProblemRef.favoriteId(): String =
    catalogKey?.let { CATALOG_PREFIX + it } ?: (CUSTOM_PREFIX + customLabel.orEmpty())

fun favoriteRef(id: String): ProblemRef? = when {
    id.startsWith(CATALOG_PREFIX) -> ProblemRef(catalogKey = id.removePrefix(CATALOG_PREFIX))
    id.startsWith(CUSTOM_PREFIX) -> ProblemRef(customLabel = id.removePrefix(CUSTOM_PREFIX))
    else -> null
}
