package fr.conscience.numerique

import android.content.Context
import androidx.room.Room
import fr.conscience.numerique.data.AppDatabase
import fr.conscience.numerique.data.AppRepository
import fr.conscience.numerique.data.InstalledAppsProvider
import fr.conscience.numerique.data.MIGRATION_1_2
import fr.conscience.numerique.data.MIGRATION_2_3
import fr.conscience.numerique.data.MIGRATION_3_4
import fr.conscience.numerique.data.MIGRATION_4_5
import fr.conscience.numerique.data.MIGRATION_5_6
import fr.conscience.numerique.data.SettingsStore
import fr.conscience.numerique.service.FrictionGate

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
}
