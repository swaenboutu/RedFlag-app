package fr.conscience.numerique.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MonitoredApp::class,
        Problem::class,
        CustomProblem::class,
        CatalogOverride::class,
        ChoiceEvent::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun choiceEventDao(): ChoiceEventDao
}

/** v2 : table des problématiques personnalisées, alimentée avec celles déjà associées à des apps. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_problems` (`label` TEXT NOT NULL, PRIMARY KEY(`label`))")
        db.execSQL(
            "INSERT OR IGNORE INTO custom_problems (label) " +
                "SELECT DISTINCT customLabel FROM problems WHERE customLabel IS NOT NULL",
        )
    }
}

/** v3 : intitulés personnalisés des problématiques du catalogue. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `catalog_overrides` " +
                "(`catalogKey` TEXT NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`catalogKey`))",
        )
    }
}
