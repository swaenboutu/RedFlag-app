package app.redflag

import android.content.Context
import androidx.room.Room
import app.redflag.data.AppDatabase
import app.redflag.data.AppIconCache
import app.redflag.data.AppRepository
import app.redflag.data.InstalledApp
import app.redflag.data.InstalledAppsProvider
import app.redflag.data.MIGRATION_1_2
import app.redflag.data.MIGRATION_2_3
import app.redflag.data.MIGRATION_3_4
import app.redflag.data.MIGRATION_4_5
import app.redflag.data.MIGRATION_5_6
import app.redflag.data.MIGRATION_6_7
import app.redflag.data.MIGRATION_7_8
import app.redflag.data.SettingsStore
import app.redflag.service.FrictionGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Minimal dependency root, with no injection framework. [database]: the database; a test passes an in-memory one so as not to touch
 * the user's.
 */
class AppContainer(context: Context, database: AppDatabase = openDatabase(context)) {
    val settings = SettingsStore(context)
    val repository = AppRepository(database)
    val icons = AppIconCache(context)
    val installedApps = InstalledAppsProvider(context, icons)
    val frictionGate = FrictionGate()

    /** For what must finish even if the screen that started it goes away (a database write, for example). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The apps to list: the "Displayed list" setting applies, but a flagged app is always there (otherwise it would stay watched
     * with no way to find it again). Updated when the setting or the flagged apps change, and at each emission of [refresh] (back
     * on the screen: an app may have been installed in the meantime).
     */
    fun installedAppsFlow(refresh: Flow<Int> = flowOf(0)): Flow<List<InstalledApp>> =
        listing(refresh) { hide, flagged -> installedApps.list(hide, alwaysInclude = flagged) }

    /** The number of apps [installedAppsFlow] lists, without loading their icons (for the statistics). */
    fun installedAppsCountFlow(): Flow<Int> = listing(flowOf(0)) { hide, flagged -> installedApps.count(hide, alwaysInclude = flagged) }

    private fun <T> listing(refresh: Flow<Int>, read: (hideSystemApps: Boolean, flagged: Set<String>) -> T): Flow<T> = combine(
        settings.hideSystemApps,
        repository.monitoredApps.map { list -> list.map { it.app.packageName }.toSet() }.distinctUntilChanged(),
        refresh,
    ) { hide, flagged, _ -> hide to flagged }
        .map { (hide, flagged) -> withContext(Dispatchers.IO) { read(hide, flagged) } }
}

private fun openDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "conscience.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
        .build()
