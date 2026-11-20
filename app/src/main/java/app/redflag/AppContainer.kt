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
import app.redflag.data.MIGRATION_8_9
import app.redflag.data.HistoryCompaction
import app.redflag.data.SettingsStore
import app.redflag.data.packageChanges
import app.redflag.service.FrictionGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Minimal dependency root, with no injection framework. [database]: the database; a test passes an in-memory one so as not to touch
 * the user's. [packageEvents]: the apps installed, removed or updated (a test can pass its own).
 */
class AppContainer(
    context: Context,
    database: AppDatabase = openDatabase(context),
    packageEvents: Flow<String> = packageChanges(context),
) {
    val settings = SettingsStore(context)
    val repository = AppRepository(database)
    val icons = AppIconCache(context)
    val installedApps = InstalledAppsProvider(context, icons)
    val frictionGate = FrictionGate()

    /** For what must finish even if the screen that started it goes away (a database write, for example). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Emits when an app is installed, removed or updated, for every screen at once; the system is only listened to while a screen
     * listens. The icon of the app is forgotten first, so that an update that changes it shows the new one.
     */
    val appsChanged: SharedFlow<Unit> = packageEvents
        .onEach { icons.forget(it) }
        .map { }
        .shareIn(applicationScope, SharingStarted.WhileSubscribed())

    /** When the history was last compacted in this process; 0 = not yet. */
    @Volatile
    private var lastCompaction = 0L

    /**
     * Keeps the history small (see [HistoryCompaction]), at most once a day: at startup and after an answer, since the accessibility
     * service keeps the process alive for weeks. The start date shown on Home is decided first, from the oldest answer, so that it
     * does not move once that answer is only counted per month.
     */
    fun compactHistoryIfDue(now: Long = System.currentTimeMillis()) {
        if (now - lastCompaction < HistoryCompaction.EVERY_MILLIS) return
        lastCompaction = now
        applicationScope.launch {
            settings.startedAt(repository.firstChoiceTimestamp() ?: now)
            repository.compactHistory(now)
        }
    }

    /**
     * The apps to list: the "Displayed list" setting applies, but a flagged app is always there (otherwise it would stay watched
     * with no way to find it again). Updated when the setting or the flagged apps change, and when an app is installed, removed or
     * updated ([appsChanged]): nothing is read again just because a screen comes back.
     */
    fun installedAppsFlow(): Flow<List<InstalledApp>> = listing { hide, flagged -> installedApps.list(hide, alwaysInclude = flagged) }

    /** The number of apps [installedAppsFlow] lists, without loading their icons (for the statistics). */
    fun installedAppsCountFlow(): Flow<Int> = listing { hide, flagged -> installedApps.count(hide, alwaysInclude = flagged) }

    /** An install sends several events in a row (added, then changed): the list is read once, after the last one. */
    @OptIn(FlowPreview::class)
    private val appsChangedOnce: Flow<Unit> get() = appsChanged.debounce(APPS_CHANGED_DEBOUNCE_MS).onStart { emit(Unit) }

    private fun <T> listing(read: (hideSystemApps: Boolean, flagged: Set<String>) -> T): Flow<T> = combine(
        settings.hideSystemApps,
        repository.monitoredApps.map { list -> list.map { it.app.packageName }.toSet() }.distinctUntilChanged(),
        appsChangedOnce,
    ) { hide, flagged, _ -> hide to flagged }
        .map { (hide, flagged) -> withContext(Dispatchers.IO) { read(hide, flagged) } }

    private companion object {
        const val APPS_CHANGED_DEBOUNCE_MS = 500L
    }
}

private fun openDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "conscience.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .build()
