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

/** Liste des problématiques personnalisées connues, indépendamment des apps auxquelles elles sont associées. */
@Entity(tableName = "custom_problems")
data class CustomProblem(@PrimaryKey val label: String)

/** Intitulé choisi par l'utilisateur pour une problématique du catalogue ; remplace la traduction, dans toutes les langues. */
@Entity(tableName = "catalog_overrides")
data class CatalogOverride(@PrimaryKey val catalogKey: String, val label: String)

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

fun Problem.matches(ref: ProblemRef) = catalogKey == ref.catalogKey && customLabel == ref.customLabel
