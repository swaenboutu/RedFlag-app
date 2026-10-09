package app.redflag.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Les apps surveillées et leurs problématiques associées. */
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

    @Upsert
    abstract suspend fun upsert(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET snoozedUntil = :until WHERE packageName = :packageName")
    abstract suspend fun snooze(packageName: String, until: Long)

    @Insert
    protected abstract suspend fun insertProblems(problems: List<Problem>)

    @Query("DELETE FROM problems WHERE problemId = :problemId")
    abstract suspend fun deleteProblemsWithId(problemId: String)

    @Query("DELETE FROM monitored_apps WHERE packageName NOT IN (SELECT packageName FROM problems)")
    abstract suspend fun deleteAppsWithoutProblems()

    @Query("SELECT COUNT(*) FROM monitored_apps WHERE packageName = :packageName")
    protected abstract suspend fun appCount(packageName: String): Int

    @Query("SELECT COUNT(*) FROM problems WHERE packageName = :packageName AND problemId = :problemId")
    protected abstract suspend fun problemCount(packageName: String, problemId: String): Int

    @Query("DELETE FROM problems WHERE packageName = :packageName AND problemId = :problemId")
    protected abstract suspend fun deleteProblem(packageName: String, problemId: String)

    /** Associe une problématique à une app (créée si besoin, sans toucher à sa pause si elle existe). */
    @Transaction
    open suspend fun link(app: MonitoredApp, problem: Problem) {
        if (appCount(app.packageName) == 0) upsert(app)
        if (problemCount(problem.packageName, problem.problemId) == 0) {
            insertProblems(listOf(problem))
        }
    }

    /** Dissocie ; une app qui n'a plus aucune problématique sort de la surveillance. */
    @Transaction
    open suspend fun unlink(packageName: String, problemId: String) {
        deleteProblem(packageName, problemId)
        deleteAppsWithoutProblems()
    }
}

/** Les problématiques créées par l'utilisateur. */
@Dao
interface CustomProblemDao {
    @Query("SELECT * FROM custom_problems ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomProblem>>

    @Query("SELECT * FROM custom_problems")
    suspend fun listOnce(): List<CustomProblem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(problem: CustomProblem)

    @Query("UPDATE custom_problems SET label = :label WHERE id = :id")
    suspend fun rename(id: String, label: String)

    @Query("DELETE FROM custom_problems WHERE id = :id")
    suspend fun delete(id: String)
}

/** Les problématiques mises en favoris. */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun observeAll(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: String)
}

/** Les intitulés choisis par l'utilisateur à la place de ceux du catalogue. */
@Dao
interface CatalogOverrideDao {
    @Query("SELECT * FROM catalog_overrides")
    fun observeAll(): Flow<List<CatalogOverride>>

    @Upsert
    suspend fun upsert(override: CatalogOverride)

    @Query("DELETE FROM catalog_overrides WHERE catalogKey = :catalogKey")
    suspend fun delete(catalogKey: String)
}

@Dao
interface ChoiceEventDao {
    @Insert
    suspend fun insert(event: ChoiceEvent)

    @Query("SELECT * FROM choice_events ORDER BY timestamp")
    fun observeAll(): Flow<List<ChoiceEvent>>

    @Query(
        "SELECT packageName, COUNT(*) AS refusals FROM choice_events " +
            "WHERE proceeded = 0 GROUP BY packageName",
    )
    fun observeRefusals(): Flow<List<RefusalCount>>
}
