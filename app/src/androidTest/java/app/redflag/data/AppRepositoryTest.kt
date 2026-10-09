package app.redflag.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.ui.problems.toRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    private val fomo = ProblemRef.catalog("fomo")
    private val sexism = ProblemRef.catalog("sexism")

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
        assertNotNull(repository.addCustomProblem("Trop de pubs"))
        assertNull(repository.addCustomProblem("trop de PUBS"))
        assertEquals(1, repository.customProblems.first().size)
    }

    @Test
    fun aNewCustomProblemIsAutomaticallyAFavorite() = run {
        val ref = repository.addCustomProblem("Mon enjeu")!!

        assertEquals(setOf(ref), repository.favorites.first())

        // A refused duplicate adds nothing, not even a favorite.
        assertNull(repository.addCustomProblem("mon ENJEU"))
        assertEquals(setOf(ref), repository.favorites.first())

        // It can still be removed from the favorites afterwards.
        repository.setFavorite(ref, false)
        assertTrue(repository.favorites.first().isEmpty())
    }

    @Test
    fun eachCustomProblemGetsItsOwnIdentifier() = run {
        val first = repository.addCustomProblem("Un")!!
        val second = repository.addCustomProblem("Deux")!!

        assertTrue(first.isCustom && second.isCustom)
        assertNotEquals(first, second)
        assertEquals(setOf(first.id, second.id), repository.customProblems.first().map { it.id }.toSet())
    }

    @Test
    fun aCustomProblemKeepsItsTheme() = run {
        repository.addCustomProblem("Mon souci", category = "privacy")

        val custom = repository.customProblems.first().single()
        assertEquals("privacy", custom.category)
    }

    @Test
    fun renamingACustomProblemUpdatesLinksFavoritesAndKeepsTheTheme() = run {
        val ref = repository.addCustomProblem("ancien", category = "privacy")!!
        repository.linkProblem("app.a", "App A", ref)
        repository.setFavorite(ref, true)

        assertTrue(repository.renameCustomProblem(ref.id, "nouveau"))

        // L'identifiant ne change pas : les liens et les favoris suivent sans rien à mettre à jour.
        assertEquals(listOf(ref), repository.find("app.a")?.problems?.map { it.toRef() })
        assertTrue(ref in repository.favorites.first())
        val custom = repository.customProblems.first().single()
        assertEquals(ref.id, custom.id)
        assertEquals("nouveau", custom.label)
        assertEquals("privacy", custom.category)
        assertEquals("nouveau", repository.labelOverrides.first()[ref.id])
    }

    @Test
    fun renamingToAnExistingNameIsRefused() = run {
        val un = repository.addCustomProblem("un")!!
        repository.addCustomProblem("deux")

        assertFalse(repository.renameCustomProblem(un.id, "DEUX"))
        assertEquals(setOf("un", "deux"), repository.customProblems.first().map { it.label }.toSet())
        assertTrue("garder son propre nom, à la casse près, est permis", repository.renameCustomProblem(un.id, "UN"))
    }

    @Test
    fun deletingACustomProblemRemovesItEverywhere() = run {
        val custom = repository.addCustomProblem("a supprimer")!!
        repository.linkProblem("app.a", "App A", custom)
        repository.linkProblem("app.a", "App A", fomo)
        repository.setFavorite(custom, true)

        repository.deleteCustomProblem(custom.id)

        assertTrue(repository.customProblems.first().isEmpty())
        assertEquals(listOf(fomo), repository.find("app.a")?.problems?.map { it.toRef() })
        assertFalse(custom in repository.favorites.first())

        // Une app dont c'était la seule problématique sort de la surveillance.
        val only = repository.addCustomProblem("seule")!!
        repository.linkProblem("app.b", "App B", only)
        repository.deleteCustomProblem(only.id)
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
