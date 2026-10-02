package fr.conscience.numerique.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    /** Timestamp (ms) jusqu'auquel la friction est suspendue pour cette app ; 0 = jamais suspendue. */
    val snoozedUntil: Long = 0,
)

/**
 * Une problématique associée à une app. [problemId] est son identifiant, le même pour toutes : la clé d'une entrée du
 * catalogue (`fomo`, traduite à l'affichage) ou l'identifiant d'une problématique personnalisée (`custom:…`, voir [CustomProblem]).
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
    val problemId: String,
)

/**
 * Liste des problématiques personnalisées connues, indépendamment des apps auxquelles elles sont associées.
 * [id] : identifiant stable (`custom:…`), qui ne change jamais ; [label] : le nom affiché, que l'utilisateur peut modifier.
 * [category] : clé du thème du catalogue où la ranger ; null = thème « Personnalisé ».
 */
@Entity(tableName = "custom_problems")
data class CustomProblem(@PrimaryKey val id: String, val label: String, val category: String? = null)

/** Intitulé choisi par l'utilisateur pour une problématique du catalogue ; remplace la traduction, dans toutes les langues. */
@Entity(tableName = "catalog_overrides")
data class CatalogOverride(@PrimaryKey val catalogKey: String, val label: String)

/** Problématique mise en favori ; [id] est l'identifiant de la problématique. */
@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val id: String)

@Entity(tableName = "choice_events", indices = [Index("packageName")])
data class ChoiceEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    /** true = « Oui, continuer », false = « Non, revenir en arrière ». */
    val proceeded: Boolean,
    /** true = « Ne plus demander pendant… » (la pause est comptée comme un passage, mais distinguée dans les statistiques). */
    @ColumnInfo(defaultValue = "0") val snoozed: Boolean = false,
)

data class MonitoredAppWithProblems(
    @Embedded val app: MonitoredApp,
    @Relation(parentColumn = "packageName", entityColumn = "packageName")
    val problems: List<Problem>,
)

data class RefusalCount(val packageName: String, val refusals: Int)

/**
 * Identifie une problématique, du catalogue ou personnalisée : un seul identifiant [id] pour toutes.
 * Celui d'une entrée du catalogue est sa clé (`fomo`) ; celui d'une personnalisée commence par `custom:` et est généré une fois pour toutes.
 */
data class ProblemRef(val id: String) {
    val isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

    /** La clé du catalogue, ou null pour une problématique personnalisée. */
    val catalogKey: String? get() = id.takeUnless { isCustom }

    companion object {
        /** Une entrée du catalogue, par sa clé. */
        fun catalog(key: String) = ProblemRef(key)

        /** Une nouvelle problématique personnalisée, avec un identifiant qui n'existait pas encore. */
        fun newCustom() = ProblemRef(CUSTOM_PREFIX + UUID.randomUUID().toString().replace("-", ""))
    }
}

const val CUSTOM_PREFIX = "custom:"

fun Problem.toRef() = ProblemRef(problemId)

fun Problem.matches(ref: ProblemRef) = problemId == ref.id
