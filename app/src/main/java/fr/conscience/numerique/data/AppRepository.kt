package fr.conscience.numerique.data

import fr.conscience.numerique.util.alphabetical
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(private val db: AppDatabase) {
    private val apps get() = db.monitoredAppDao()
    private val events get() = db.choiceEventDao()

    val monitoredApps: Flow<List<MonitoredAppWithProblems>> get() = apps.observeAll()
    val customLabels: Flow<List<String>> get() = apps.observeCustomLabels().map { it.sortedWith(alphabetical()) }
    val customProblems: Flow<List<CustomProblem>> get() = apps.observeCustomProblems().map { list -> list.sortedWith(compareBy(alphabetical()) { it.label }) }
    val problems: Flow<List<Problem>> get() = apps.observeProblems()
    val favorites: Flow<Set<ProblemRef>>
        get() = apps.observeFavorites().map { list -> list.mapNotNull { favoriteRef(it.id) }.toSet() }
    val labelOverrides: Flow<Map<String, String>>
        get() = apps.observeOverrides().map { list -> list.associate { it.catalogKey to it.label } }
    val refusals: Flow<List<RefusalCount>> get() = events.observeRefusals()
    val choiceEvents: Flow<List<ChoiceEvent>> get() = events.observeAll()

    suspend fun find(packageName: String): MonitoredAppWithProblems? = apps.find(packageName)

    /** Ajoute une problématique personnalisée à la liste, dans le thème [category] (null = « Personnalisé ») ; false si elle existe déjà (casse ignorée). */
    suspend fun addCustomProblem(label: String, category: String? = null): Boolean {
        if (apps.customLabelsOnce().any { it.equals(label, ignoreCase = true) }) return false
        apps.insertCustomLabels(listOf(CustomProblem(label, category)))
        return true
    }

    /** Renomme partout ; false si le nouveau nom est déjà pris par une autre problématique. */
    suspend fun renameCustomProblem(oldLabel: String, newLabel: String): Boolean {
        if (oldLabel == newLabel) return true
        val taken = apps.customLabelsOnce().any { it != oldLabel && it.equals(newLabel, ignoreCase = true) }
        if (taken) return false
        apps.renameCustom(oldLabel, newLabel)
        return true
    }

    suspend fun deleteCustomProblem(label: String) = apps.deleteCustom(label)

    suspend fun setFavorite(ref: ProblemRef, favorite: Boolean) {
        if (favorite) apps.insertFavorite(Favorite(ref.favoriteId())) else apps.deleteFavorite(ref.favoriteId())
    }

    suspend fun setCatalogLabel(catalogKey: String, label: String) =
        apps.upsertOverride(CatalogOverride(catalogKey, label))

    suspend fun resetCatalogLabel(catalogKey: String) = apps.deleteOverride(catalogKey)

    suspend fun linkProblem(packageName: String, appName: String, ref: ProblemRef) =
        apps.link(
            MonitoredApp(packageName, appName),
            Problem(packageName = packageName, catalogKey = ref.catalogKey, customLabel = ref.customLabel),
        )

    suspend fun unlinkProblem(packageName: String, ref: ProblemRef) =
        apps.unlink(packageName, ref.catalogKey, ref.customLabel)

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
