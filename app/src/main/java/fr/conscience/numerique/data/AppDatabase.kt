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
        Favorite::class,
        ChoiceEvent::class,
    ],
    version = 6,
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

/** v4 : problématiques favorites. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}
/** v5 : thème (facultatif) d'une problématique personnalisée. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `custom_problems` ADD COLUMN `category` TEXT")
    }
}

/** v6 : les événements distinguent une mise en pause (« Ne plus demander ») d'un simple « Oui ». */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `choice_events` ADD COLUMN `snoozed` INTEGER NOT NULL DEFAULT 0")
    }
}

