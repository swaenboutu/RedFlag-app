package fr.conscience.numerique.data

import androidx.room.withTransaction
import fr.conscience.numerique.util.alphabetical
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
     * Les intitulés qui remplacent la traduction, par identifiant de problématique : celui que l'utilisateur a choisi pour une
     * problématique du catalogue, et le nom de chaque problématique personnalisée (qui n'en a pas d'autre).
     */
    val labelOverrides: Flow<Map<String, String>>
        get() = combine(overrides.observeAll(), custom.observeAll()) { renamed, created ->
            renamed.associate { it.catalogKey to it.label } + created.associate { it.id to it.label }
        }
    val refusals: Flow<List<RefusalCount>> get() = events.observeRefusals()
    val choiceEvents: Flow<List<ChoiceEvent>> get() = events.observeAll()

    suspend fun find(packageName: String): MonitoredAppWithProblems? = apps.find(packageName)

    /**
     * Ajoute une problématique personnalisée à la liste, dans le thème [category] (null = « Personnalisé ») et lui donne son
     * identifiant. Null si le nom existe déjà (casse ignorée).
     */
    suspend fun addCustomProblem(label: String, category: String? = null): ProblemRef? {
        if (custom.listOnce().any { it.label.equals(label, ignoreCase = true) }) return null
        val ref = ProblemRef.newCustom()
        custom.insert(CustomProblem(ref.id, label, category))
        return ref
    }

    /** Renomme : l'identifiant ne change pas, rien d'autre n'est à mettre à jour. False si le nom est déjà pris par une autre problématique. */
    suspend fun renameCustomProblem(id: String, newLabel: String): Boolean {
        val taken = custom.listOnce().any { it.id != id && it.label.equals(newLabel, ignoreCase = true) }
        if (taken) return false
        custom.rename(id, newLabel)
        return true
    }

    /** Retire la problématique de toutes les apps (celles qui n'en ont plus sortent de la surveillance), des favoris et de la liste. */
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
