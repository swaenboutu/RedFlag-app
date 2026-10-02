package fr.conscience.numerique

import android.content.Context
import androidx.room.Room
import fr.conscience.numerique.data.AppDatabase
import fr.conscience.numerique.data.AppRepository
import fr.conscience.numerique.data.InstalledApp
import fr.conscience.numerique.data.InstalledAppsProvider
import fr.conscience.numerique.data.MIGRATION_1_2
import fr.conscience.numerique.data.MIGRATION_2_3
import fr.conscience.numerique.data.MIGRATION_3_4
import fr.conscience.numerique.data.MIGRATION_4_5
import fr.conscience.numerique.data.MIGRATION_5_6
import fr.conscience.numerique.data.SettingsStore
import fr.conscience.numerique.service.FrictionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Racine de dépendances minimale, sans framework d'injection. */
class AppContainer(context: Context) {
    private val database: AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "conscience.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    val settings = SettingsStore(context)
    val repository = AppRepository(database)
    val installedApps = InstalledAppsProvider(context)
    val frictionGate = FrictionGate()

    /**
     * Les apps à lister : le réglage « Liste affichée » est appliqué, mais une app signalée y figure toujours (sinon elle
     * resterait surveillée sans qu'on puisse la retrouver). Se met à jour quand le réglage ou les apps signalées changent,
     * et à chaque émission de [refresh] (au retour sur l'écran : une app a pu être installée entre-temps).
     */
    fun installedAppsFlow(refresh: Flow<Int> = flowOf(0)): Flow<List<InstalledApp>> = combine(
        settings.hideSystemApps,
        repository.monitoredApps.map { list -> list.map { it.app.packageName }.toSet() }.distinctUntilChanged(),
        refresh,
    ) { hide, flagged, _ -> hide to flagged }
        .map { (hide, flagged) -> withContext(Dispatchers.IO) { installedApps.list(hide, alwaysInclude = flagged) } }
}
