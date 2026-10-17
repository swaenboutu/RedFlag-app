package app.redflag.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The watched apps and the issues linked to them. */
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

    /** Links an issue to an app (created if needed, without touching its pause if it exists). */
    @Transaction
    open suspend fun link(app: MonitoredApp, problem: Problem) {
        if (appCount(app.packageName) == 0) upsert(app)
        if (problemCount(problem.packageName, problem.problemId) == 0) {
            insertProblems(listOf(problem))
        }
    }

    /** Unlinks; an app with no issue left is no longer watched. */
    @Transaction
    open suspend fun unlink(packageName: String, problemId: String) {
        deleteProblem(packageName, problemId)
        deleteAppsWithoutProblems()
    }
}

/** The issues created by the user. */
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

/** The favorite issues. */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun observeAll(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: String)
}

/** The titles the user chose instead of the catalog ones. */
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

    /** The time of the oldest choice, null when there is none. */
    @Query("SELECT MIN(timestamp) FROM choice_events")
    suspend fun firstTimestamp(): Long?

    @Query(
        "SELECT packageName, COUNT(*) AS refusals FROM choice_events " +
            "WHERE proceeded = 0 GROUP BY packageName",
    )
    fun observeRefusals(): Flow<List<RefusalCount>>
}
