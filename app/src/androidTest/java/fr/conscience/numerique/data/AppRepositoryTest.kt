package fr.conscience.numerique.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Le dépôt de données sur une vraie base SQLite (en mémoire) : associations, problématiques personnalisées, favoris, historique. */
@RunWith(AndroidJUnit4::class)
class AppRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    private val fomo = ProblemRef(catalogKey = "fomo")
    private val sexism = ProblemRef(catalogKey = "sexism")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    @Test
    fun linkingAProblemSurveillesTheAppWithoutDuplicates() = run {
        repository.linkProblem("app.a", "App A", fomo)

        val app = repository.find("app.a")
        assertEquals("App A", app?.app?.appName)
        assertEquals(listOf(fomo), app?.problems?.map { it.toRef() })

        // Associer deux fois la même problématique ne crée pas de doublon.
        repository.linkProblem("app.a", "App A", fomo)
        assertEquals(1, repository.find("app.a")?.problems?.size)
    }

    @Test
    fun anAppCanHaveSeveralProblemsAndAProblemSeveralApps() = run {
        repository.linkProblem("app.a", "App A", fomo)
        repository.linkProblem("app.a", "App A", sexism)
        repository.linkProblem("app.b", "App B", fomo)

        assertEquals(2, repository.find("app.a")?.problems?.size)
        assertEquals(2, repository.monitoredApps.first().size)
    }

    @Test
    fun unlinkingTheLastProblemStopsSurveillingTheApp() = run {
        repository.linkProblem("app.a", "App A", fomo)
        repository.linkProblem("app.a", "App A", sexism)

        repository.unlinkProblem("app.a", fomo)
        assertEquals(1, repository.find("app.a")?.problems?.size)

        repository.unlinkProblem("app.a", sexism)
        assertNull("plus aucune problématique : l'app sort de la surveillance", repository.find("app.a"))
    }

    @Test
    fun unlinkingOneAppLeavesTheOthersUntouched() = run {
        repository.linkProblem("app.a", "App A", fomo)
        repository.linkProblem("app.b", "App B", fomo)

        repository.unlinkProblem("app.a", fomo)

        assertNull(repository.find("app.a"))
        assertNotNull(repository.find("app.b"))
    }

    @Test
    fun customProblemNamesAreUniqueIgnoringCase() = run {
        assertTrue(repository.addCustomProblem("Trop de pubs"))
        assertFalse(repository.addCustomProblem("trop de PUBS"))
        assertEquals(1, repository.customLabels.first().size)
    }

    @Test
    fun aCustomProblemKeepsItsTheme() = run {
        repository.addCustomProblem("Mon souci", category = "privacy")

        val custom = repository.customProblems.first().single()
        assertEquals("privacy", custom.category)
    }

    @Test
    fun renamingACustomProblemUpdatesLinksFavoritesAndKeepsTheTheme() = run {
        val old = ProblemRef(customLabel = "ancien")
        repository.addCustomProblem("ancien", category = "privacy")
        repository.linkProblem("app.a", "App A", old)
        repository.setFavorite(old, true)

        assertTrue(repository.renameCustomProblem("ancien", "nouveau"))

        val renamed = ProblemRef(customLabel = "nouveau")
        assertEquals(listOf(renamed), repository.find("app.a")?.problems?.map { it.toRef() })
        assertTrue(renamed in repository.favorites.first())
        assertFalse(old in repository.favorites.first())
        assertEquals("privacy", repository.customProblems.first().single().category)
    }

    @Test
    fun renamingToAnExistingNameIsRefused() = run {
        repository.addCustomProblem("un")
        repository.addCustomProblem("deux")

        assertFalse(repository.renameCustomProblem("un", "DEUX"))
        assertEquals(setOf("un", "deux"), repository.customLabels.first().toSet())
    }

    @Test
    fun deletingACustomProblemRemovesItEverywhere() = run {
        val custom = ProblemRef(customLabel = "a supprimer")
        repository.addCustomProblem("a supprimer")
        repository.linkProblem("app.a", "App A", custom)
        repository.linkProblem("app.a", "App A", fomo)
        repository.setFavorite(custom, true)

        repository.deleteCustomProblem("a supprimer")

        assertTrue(repository.customLabels.first().isEmpty())
        assertEquals(listOf(fomo), repository.find("app.a")?.problems?.map { it.toRef() })
        assertFalse(custom in repository.favorites.first())

        // Une app dont c'était la seule problématique sort de la surveillance.
        val only = ProblemRef(customLabel = "seule")
        repository.addCustomProblem("seule")
        repository.linkProblem("app.b", "App B", only)
        repository.deleteCustomProblem("seule")
        assertNull(repository.find("app.b"))
    }

    @Test
    fun favoritesCanBeAddedAndRemoved() = run {
        repository.setFavorite(fomo, true)
        repository.setFavorite(fomo, true)
        assertEquals(setOf(fomo), repository.favorites.first())

        repository.setFavorite(fomo, false)
        assertTrue(repository.favorites.first().isEmpty())
    }

    @Test
    fun aCatalogLabelCanBeOverriddenAndRestored() = run {
        repository.setCatalogLabel("fomo", "Peur de rater")
        assertEquals(mapOf("fomo" to "Peur de rater"), repository.labelOverrides.first())

        repository.setCatalogLabel("fomo", "Autre texte")
        assertEquals(mapOf("fomo" to "Autre texte"), repository.labelOverrides.first())

        repository.resetCatalogLabel("fomo")
        assertTrue(repository.labelOverrides.first().isEmpty())
    }

    @Test
    fun snoozingAnAppRecordsTheEndOfThePause() = run {
        repository.linkProblem("app.a", "App A", fomo)

        repository.snooze("app.a", 42_000L)

        assertEquals(42_000L, repository.find("app.a")?.app?.snoozedUntil)
    }

    @Test
    fun choicesAreRecordedWithTheirKind() = run {
        repository.recordChoice("app.a", proceeded = false)
        repository.recordChoice("app.a", proceeded = true)
        repository.recordChoice("app.a", proceeded = true, snoozed = true)

        val events = repository.choiceEvents.first()
        assertEquals(3, events.size)
        assertEquals(1, events.count { !it.proceeded })
        assertEquals(1, events.count { it.proceeded && !it.snoozed })
        assertEquals(1, events.count { it.snoozed })
        assertEquals(listOf(RefusalCount("app.a", 1)), repository.refusals.first())
    }

    @Test
    fun historyIsKeptWhenAnAppLeavesTheSurveillance() = run {
        repository.linkProblem("app.a", "App A", fomo)
        repository.recordChoice("app.a", proceeded = false)

        repository.unlinkProblem("app.a", fomo)

        assertEquals(1, repository.choiceEvents.first().size)
    }
}
