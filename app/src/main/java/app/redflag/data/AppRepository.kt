package app.redflag.data

import androidx.room.withTransaction
import app.redflag.util.alphabetical
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AppRepository(private val db: AppDatabase) {
    private val apps get() = db.monitoredAppDao()
    private val custom get() = db.customProblemDao()
    private val favoriteDao get() = db.favoriteDao()
    private val overrides get() = db.catalogOverrideDao()
    private val events get() = db.choiceEventDao()

    val monitoredApps: Flow<List<MonitoredAppWithProblems>> get() = apps.observeAll()
    val customProblems: Flow<List<CustomProblem>> get() = custom.observeAll().map { list -> list.sortedWith(compareBy(alphabetical()) { it.label }) }
    val problems: Flow<List<Problem>> get() = apps.observeProblems()
    val favorites: Flow<Set<ProblemRef>>
        get() = favoriteDao.observeAll().map { list -> list.map { ProblemRef(it.id) }.toSet() }

    /**
     * The titles that replace the translation, by issue identifier: the one the user chose for a catalog issue, and the name of
     * each custom issue (which has no other).
     */
    val labelOverrides: Flow<Map<String, String>>
        get() = combine(overrides.observeAll(), custom.observeAll()) { renamed, created ->
            renamed.associate { it.catalogKey to it.label } + created.associate { it.id to it.label }
        }
    val refusals: Flow<List<RefusalCount>> get() = events.observeRefusals()
    val choiceEvents: Flow<List<ChoiceEvent>> get() = events.observeAll()

    /** The time of the first choice ever made, null before any. */
    suspend fun firstChoiceTimestamp(): Long? = events.firstTimestamp()

    suspend fun find(packageName: String): MonitoredAppWithProblems? = apps.find(packageName)

    /**
     * Adds a custom issue to the list, in the theme [category] (null = "Custom"), gives it its identifier and puts it in the
     * favorites: someone who writes their own issue cares about it. Null if the name already exists (case ignored).
     */
    suspend fun addCustomProblem(label: String, category: String? = null, description: String? = null): ProblemRef? {
        if (custom.listOnce().any { it.label.equals(label, ignoreCase = true) }) return null
        val ref = ProblemRef.newCustom()
        db.withTransaction {
            custom.insert(CustomProblem(ref.id, label, category, description))
            favoriteDao.insert(Favorite(ref.id))
        }
        return ref
    }

    /** Renames: the identifier does not change, nothing else needs updating. False if another issue already has the name. */
    suspend fun renameCustomProblem(id: String, newLabel: String): Boolean {
        val taken = custom.listOnce().any { it.id != id && it.label.equals(newLabel, ignoreCase = true) }
        if (taken) return false
        custom.rename(id, newLabel)
        return true
    }

    /** Removes the issue from every app (those left with none are no longer watched), from the favorites and from the list. */
    suspend fun deleteCustomProblem(id: String) = db.withTransaction {
        apps.deleteProblemsWithId(id)
        custom.delete(id)
        favoriteDao.delete(id)
        apps.deleteAppsWithoutProblems()
    }

    suspend fun setFavorite(ref: ProblemRef, favorite: Boolean) {
        if (favorite) favoriteDao.insert(Favorite(ref.id)) else favoriteDao.delete(ref.id)
    }

    suspend fun setCatalogLabel(catalogKey: String, label: String) =
        overrides.upsert(CatalogOverride(catalogKey, label))

    suspend fun resetCatalogLabel(catalogKey: String) = overrides.delete(catalogKey)

    suspend fun linkProblem(packageName: String, appName: String, ref: ProblemRef) =
        apps.link(
            MonitoredApp(packageName, appName),
            Problem(packageName = packageName, problemId = ref.id),
        )

    suspend fun unlinkProblem(packageName: String, ref: ProblemRef) =
        apps.unlink(packageName, ref.id)

    suspend fun snooze(packageName: String, until: Long) = apps.snooze(packageName, until)

    suspend fun recordChoice(packageName: String, proceeded: Boolean, snoozed: Boolean = false) =
        events.insert(
            ChoiceEvent(
                packageName = packageName,
                timestamp = System.currentTimeMillis(),
                proceeded = proceeded,
                snoozed = snoozed,
            ),
        )
}
