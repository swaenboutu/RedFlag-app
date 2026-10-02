package fr.conscience.numerique.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.widget.TextView
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.data.ProblemRef
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'écran d'accueil (liste des apps) et l'écran des statistiques, affichés pour de bon : ce que les ViewModels calculent
 * arrive bien à l'écran. Le calcul lui-même est vérifié dans les tests des ViewModels.
 */
@RunWith(AndroidJUnit4::class)
class HomeAndStatsScreensTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = context.container.repository
    private val settings get() = context.container.settings
    private val fomo = ProblemRef.catalog("fomo")
    private lateinit var pkg: String
    private var onboardingWasDone = false

    @Before
    fun setUp() = runBlocking {
        onboardingWasDone = settings.onboardingDone
        settings.onboardingDone = true // sinon l'accueil renvoie vers le parcours de première utilisation
        // Une vraie app installée (l'accueil ne liste que celles-là) que l'utilisateur n'avait pas déjà signalée.
        pkg = context.container.installedApps.list(hideSystemApps = true).first { repository.find(it.packageName) == null }.packageName
        repository.linkProblem(pkg, "Application signalée", fomo)
        repository.recordChoice(pkg, proceeded = false)
    }

    @After
    fun tearDown() = runBlocking {
        repository.unlinkProblem(pkg, fomo)
        settings.onboardingDone = onboardingWasDone
    }

    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.itemCount(listId: Int): Int {
        var count = -1
        onActivity { count = it.findViewById<RecyclerView>(listId).adapter?.itemCount ?: -1 }
        return count
    }

    /** Attend (au plus 5 s) que [condition] soit vraie : les écrans se remplissent en arrière-plan. */
    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.click(id: Int) =
        onActivity { it.findViewById<View>(id).performClick() }

    @Test
    fun homeFiltersTheListWithTheTabs() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.awaitUntil { scenario.itemCount(R.id.appList) > 0 }
            val all = scenario.itemCount(R.id.appList)
            assertTrue("la liste affiche des apps", all > 0)

            scenario.click(R.id.filterFlagged)
            scenario.awaitUntil { scenario.itemCount(R.id.appList) < all }
            val flagged = scenario.itemCount(R.id.appList)
            assertTrue("seules les apps signalées restent", flagged in 1 until all)

            scenario.click(R.id.filterUnflagged)
            scenario.awaitUntil { scenario.itemCount(R.id.appList) == all - flagged }
            assertEquals("signalées + non signalées = toutes", all, flagged + scenario.itemCount(R.id.appList))

            scenario.click(R.id.filterAll)
            scenario.awaitUntil { scenario.itemCount(R.id.appList) == all }
            assertEquals(all, scenario.itemCount(R.id.appList))
        }
    }

    @Test
    fun homeShowsTheNumberOfFlaggedAppsOnItsTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var label = ""
            scenario.awaitUntil {
                scenario.onActivity { label = it.findViewById<TextView>(R.id.filterFlagged).text.toString() }
                label.contains("(0)").not() && label.contains("(")
            }
            assertTrue("l'onglet « signalées » affiche leur nombre : $label", Regex("\\([1-9]\\d*\\)").containsMatchIn(label))
        }
    }

    @Test
    fun statisticsListTheInterceptedAppsAndTheirSummary() {
        ActivityScenario.launch(StatsActivity::class.java).use { scenario ->
            scenario.awaitUntil { scenario.itemCount(R.id.list) > 0 }
            assertTrue("l'app interceptée est listée", scenario.itemCount(R.id.list) >= 1)

            var emptyVisible = true
            var summary = ""
            scenario.onActivity {
                emptyVisible = it.findViewById<View>(R.id.emptyState).visibility == View.VISIBLE
                summary = it.findViewById<TextView>(R.id.summaryValue).text.toString()
            }
            assertEquals("pas de message « aucune app » quand il y en a", false, emptyVisible)
            assertTrue("le résumé indique « interceptées sur total » : $summary", summary.isNotBlank() && summary.first().isDigit())
        }
    }
}
