package fr.conscience.numerique.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MonitoredAppDao {
    @Transaction
    @Query("SELECT * FROM monitored_apps")
    abstract fun observeAll(): Flow<List<MonitoredAppWithProblems>>

    @Transaction
    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    abstract suspend fun find(packageName: String): MonitoredAppWithProblems?

    @Query("SELECT * FROM problems")
    abstract fun observeProblems(): Flow<List<Problem>>

    @Query("SELECT label FROM custom_problems ORDER BY label COLLATE NOCASE")
    abstract fun observeCustomLabels(): Flow<List<String>>

    @Query("SELECT label FROM custom_problems")
    abstract suspend fun customLabelsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCustomLabels(labels: List<CustomProblem>)

    @Query("SELECT * FROM favorites")
    abstract fun observeFavorites(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE id = :id")
    abstract suspend fun deleteFavorite(id: String)

    @Query("UPDATE OR REPLACE favorites SET id = :newId WHERE id = :oldId")
    protected abstract suspend fun renameFavorite(oldId: String, newId: String)

    @Query("SELECT * FROM catalog_overrides")
    abstract fun observeOverrides(): Flow<List<CatalogOverride>>

    @Upsert
    abstract suspend fun upsertOverride(override: CatalogOverride)

    @Query("DELETE FROM catalog_overrides WHERE catalogKey = :catalogKey")
    abstract suspend fun deleteOverride(catalogKey: String)

    @Upsert
    abstract suspend fun upsert(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET snoozedUntil = :until WHERE packageName = :packageName")
    abstract suspend fun snooze(packageName: String, until: Long)

    @Insert
    protected abstract suspend fun insertProblems(problems: List<Problem>)

    @Query("UPDATE problems SET customLabel = :newLabel WHERE customLabel = :oldLabel")
    protected abstract suspend fun renameInProblems(oldLabel: String, newLabel: String)

    @Query("DELETE FROM problems WHERE customLabel = :label")
    protected abstract suspend fun deleteProblemsWithLabel(label: String)

    @Query("DELETE FROM custom_problems WHERE label = :label")
    protected abstract suspend fun deleteCustomLabel(label: String)

    @Query("DELETE FROM monitored_apps WHERE packageName NOT IN (SELECT packageName FROM problems)")
    protected abstract suspend fun deleteAppsWithoutProblems()

    @Query("SELECT COUNT(*) FROM monitored_apps WHERE packageName = :packageName")
    protected abstract suspend fun appCount(packageName: String): Int

    @Query(
        "SELECT COUNT(*) FROM problems WHERE packageName = :packageName " +
            "AND catalogKey IS :catalogKey AND customLabel IS :customLabel",
    )
    protected abstract suspend fun problemCount(packageName: String, catalogKey: String?, customLabel: String?): Int

    @Query(
        "DELETE FROM problems WHERE packageName = :packageName " +
            "AND catalogKey IS :catalogKey AND customLabel IS :customLabel",
    )
    protected abstract suspend fun deleteProblem(packageName: String, catalogKey: String?, customLabel: String?)

    /** Associe une problématique à une app (créée si besoin, sans toucher à sa pause si elle existe). */
    @Transaction
    open suspend fun link(app: MonitoredApp, problem: Problem) {
        if (appCount(app.packageName) == 0) upsert(app)
        if (problemCount(problem.packageName, problem.catalogKey, problem.customLabel) == 0) {
            insertProblems(listOf(problem))
        }
    }

    /** Dissocie ; une app qui n'a plus aucune problématique sort de la surveillance. */
    @Transaction
    open suspend fun unlink(packageName: String, catalogKey: String?, customLabel: String?) {
        deleteProblem(packageName, catalogKey, customLabel)
        deleteAppsWithoutProblems()
    }

    @Transaction
    open suspend fun renameCustom(oldLabel: String, newLabel: String) {
        deleteCustomLabel(oldLabel)
        insertCustomLabels(listOf(CustomProblem(newLabel)))
        renameInProblems(oldLabel, newLabel)
        renameFavorite(CUSTOM_PREFIX + oldLabel, CUSTOM_PREFIX + newLabel)
    }

    /** Retire la problématique de toutes les apps ; celles qui n'en ont plus sortent de la surveillance. */
    @Transaction
    open suspend fun deleteCustom(label: String) {
        deleteProblemsWithLabel(label)
        deleteCustomLabel(label)
        deleteFavorite(CUSTOM_PREFIX + label)
        deleteAppsWithoutProblems()
    }
}

@Dao
interface ChoiceEventDao {
    @Insert
    suspend fun insert(event: ChoiceEvent)

    @Query(
        "SELECT packageName, COUNT(*) AS refusals FROM choice_events " +
            "WHERE proceeded = 0 GROUP BY packageName",
    )
    fun observeRefusals(): Flow<List<RefusalCount>>
}
