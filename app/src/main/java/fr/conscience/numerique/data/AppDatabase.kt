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
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun customProblemDao(): CustomProblemDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun catalogOverrideDao(): CatalogOverrideDao
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


/**
 * v7 : un seul identifiant pour toutes les problématiques. Celles du catalogue gardent leur clé ; chaque problématique
 * personnalisée, jusque-là reconnue par son texte, reçoit un identifiant généré (`custom:…`). Les tables des problématiques
 * associées aux apps (`problems`), des personnalisées (`custom_problems`) et des favoris l'utilisent à la place du texte.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Les personnalisées de la liste, puis celles seulement associées à une app (par sécurité : la liste devrait les contenir).
        db.execSQL("CREATE TABLE `custom_problems_new` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, `category` TEXT, PRIMARY KEY(`id`))")
        db.execSQL(
            "INSERT INTO custom_problems_new (id, label, category) " +
                "SELECT 'custom:' || lower(hex(randomblob(16))), label, category FROM custom_problems",
        )
        db.execSQL(
            "INSERT INTO custom_problems_new (id, label) " +
                "SELECT 'custom:' || lower(hex(randomblob(16))), customLabel FROM " +
                "(SELECT DISTINCT customLabel FROM problems WHERE customLabel IS NOT NULL " +
                "AND customLabel NOT IN (SELECT label FROM custom_problems_new))",
        )

        db.execSQL(
            "CREATE TABLE `problems_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, " +
                "`problemId` TEXT NOT NULL, FOREIGN KEY(`packageName`) REFERENCES `monitored_apps`(`packageName`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO problems_new (id, packageName, problemId) " +
                "SELECT p.id, p.packageName, COALESCE(p.catalogKey, (SELECT n.id FROM custom_problems_new n WHERE n.label = p.customLabel)) " +
                "FROM problems p WHERE p.catalogKey IS NOT NULL OR p.customLabel IS NOT NULL",
        )
        db.execSQL("DROP TABLE `problems`")
        db.execSQL("ALTER TABLE `problems_new` RENAME TO `problems`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_packageName` ON `problems` (`packageName`)")

        db.execSQL("DROP TABLE `custom_problems`")
        db.execSQL("ALTER TABLE `custom_problems_new` RENAME TO `custom_problems`")

        // Favoris : « catalog:clé » devient « clé », « custom:texte » devient l'identifiant de la personnalisée.
        db.execSQL("CREATE TABLE `favorites_new` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT OR IGNORE INTO favorites_new (id) SELECT substr(id, 9) FROM favorites WHERE id LIKE 'catalog:%'")
        db.execSQL(
            "INSERT OR IGNORE INTO favorites_new (id) " +
                "SELECT n.id FROM favorites f JOIN custom_problems n ON f.id = 'custom:' || n.label",
        )
        db.execSQL("DROP TABLE `favorites`")
        db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")
    }
}
