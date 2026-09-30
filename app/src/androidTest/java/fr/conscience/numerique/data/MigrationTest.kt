package fr.conscience.numerique.data

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrations de la base de données, de la version 1 à la version courante : les données déjà saisies par
 * l'utilisateur doivent survivre à chaque mise à jour de l'app.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val all = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

    private fun SupportSQLiteDatabase.insert(table: String, vararg values: Pair<String, Any?>) {
        val cv = ContentValues()
        values.forEach { (k, v) ->
            when (v) {
                null -> cv.putNull(k)
                is Int -> cv.put(k, v)
                is Long -> cv.put(k, v)
                is Boolean -> cv.put(k, if (v) 1 else 0)
                else -> cv.put(k, v.toString())
            }
        }
        insert(table, 0, cv)
    }

    /** Données de la version 1 : deux apps, des problématiques du catalogue et libres, des choix. */
    private fun SupportSQLiteDatabase.fillV1() {
        insert("monitored_apps", "packageName" to "app.a", "appName" to "App A", "snoozedUntil" to 0L)
        insert("monitored_apps", "packageName" to "app.b", "appName" to "App B", "snoozedUntil" to 123L)
        insert("problems", "packageName" to "app.a", "catalogKey" to "sexism", "customLabel" to null)
        insert("problems", "packageName" to "app.a", "catalogKey" to null, "customLabel" to "trop de pubs")
        insert("problems", "packageName" to "app.b", "catalogKey" to "fomo", "customLabel" to null)
        insert("choice_events", "packageName" to "app.a", "timestamp" to 1000L, "proceeded" to true)
        insert("choice_events", "packageName" to "app.a", "timestamp" to 2000L, "proceeded" to false)
    }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun migrateFromVersion1KeepsEveryUserData() {
        helper.createDatabase(name, 1).apply { fillV1(); close() }

        val db = helper.runMigrationsAndValidate(name, 6, true, *all)

        assertEquals(2, db.count("monitored_apps"))
        assertEquals(3, db.count("problems"))
        assertEquals(2, db.count("choice_events"))
        // La pause d'une app est conservée.
        db.query("SELECT snoozedUntil FROM monitored_apps WHERE packageName = 'app.b'").use {
            assertTrue(it.moveToFirst()); assertEquals(123L, it.getLong(0))
        }
    }

    @Test
    fun migrationTo2ListsCustomProblemsAlreadyLinkedToApps() {
        helper.createDatabase(name, 1).apply { fillV1(); close() }

        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)

        db.query("SELECT label FROM custom_problems").use {
            assertEquals(1, it.count)
            it.moveToFirst(); assertEquals("trop de pubs", it.getString(0))
        }
    }

    @Test
    fun migrationTo5ExistingCustomProblemsHaveNoThemeYet() {
        helper.createDatabase(name, 4).apply {
            insert("custom_problems", "label" to "mon souci")
            close()
        }

        val db = helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5)

        db.query("SELECT category FROM custom_problems WHERE label = 'mon souci'").use {
            assertTrue(it.moveToFirst()); assertTrue("pas de thème : Personnalisé", it.isNull(0))
        }
    }

    @Test
    fun migrationTo6OldChoicesAreNotMarkedAsSnoozed() {
        helper.createDatabase(name, 5).apply {
            insert("monitored_apps", "packageName" to "app.a", "appName" to "App A", "snoozedUntil" to 0L)
            insert("choice_events", "packageName" to "app.a", "timestamp" to 1000L, "proceeded" to true)
            close()
        }

        val db = helper.runMigrationsAndValidate(name, 6, true, MIGRATION_5_6)

        db.query("SELECT proceeded, snoozed FROM choice_events").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
            assertEquals(0, it.getInt(1))
        }
    }

    @Test
    fun everyVersionCanBeMigratedToTheCurrentOne() {
        for (from in 1..5) {
            helper.createDatabase("$name-$from", from).close()
            val db = helper.runMigrationsAndValidate("$name-$from", 6, true, *all)
            assertEquals("depuis la version $from", 0, db.count("monitored_apps"))
            db.close()
        }
    }

    @Test
    fun migratedDatabaseIsUsableByTheRealDatabaseClass() {
        helper.createDatabase(name, 1).apply { fillV1(); close() }
        helper.runMigrationsAndValidate(name, 6, true, *all).close()

        val db = androidx.room.Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java, name,
        ).addMigrations(*all).build()
        try {
            val dao = db.monitoredAppDao()
            val app = kotlinx.coroutines.runBlocking { dao.find("app.a") }
            assertEquals(2, app?.problems?.size)
            assertFalse(db.openHelper.writableDatabase.isReadOnly)
        } finally {
            db.close()
        }
    }
}
