package app.redflag.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.data.ProblemRef
import app.redflag.ui.apps.MainActivity
import app.redflag.ui.stats.StatsActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The list of apps and the statistics, really displayed: what the ViewModels compute does reach the screen. The computation
 * itself is checked in the ViewModel tests.
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
        settings.onboardingDone = true // otherwise the home screen sends to the welcome tour
        // A real installed app (the list only shows those) that the user had not flagged already.
        assumeUserApps(context, 1)
        pkg = context.container.installedApps.list(hideSystemApps = true).first { repository.find(it.packageName) == null }.packageName
        repository.linkProblem(pkg, "Application signalée", fomo)
        repository.recordChoice(pkg, proceeded = false)
    }

    @After
    fun tearDown() = runBlocking {
        if (::pkg.isInitialized) repository.unlinkProblem(pkg, fomo)
        settings.onboardingDone = onboardingWasDone
    }

    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.itemCount(listId: Int): Int {
        var count = -1
        onActivity { count = it.findViewById<RecyclerView>(listId).adapter?.itemCount ?: -1 }
        return count
    }

    /** Waits (at most 5 s) until [condition] is true: the screens fill in in the background. */
    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.click(id: Int) =
        onActivity { it.findViewById<View>(id).performClick() }

    private fun ActivityScenario<MainActivity>.firstVisible(): Int {
        var position = -1
        onActivity {
            val manager = it.findViewById<RecyclerView>(R.id.appList).layoutManager as LinearLayoutManager
            position = manager.findFirstVisibleItemPosition()
        }
        return position
    }

    private fun ActivityScenario<MainActivity>.text(id: Int): String {
        var text = ""
        onActivity { text = it.findViewById<TextView>(id).text.toString() }
        return text
    }

    /**
     * One scenario on the list of apps: the "flagged" tab shows their number, the tabs filter the list, and changing the tab after
     * scrolling down goes back to the top of the list.
     */
    @Test
    fun theListOfAppsCountsFiltersAndGoesBackToTheTop() {
        // With the system apps shown the list is long enough to scroll, whatever the device.
        val hideBefore = settings.hideSystemApps.value
        settings.setHideSystemApps(false)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.awaitUntil { scenario.itemCount(R.id.appList) > 10 }
                val all = scenario.itemCount(R.id.appList)
                assertTrue("la liste affiche des apps", all > 10)

                scenario.awaitUntil { Regex("\\([1-9]\\d*\\)").containsMatchIn(scenario.text(R.id.filterFlagged)) }
                val label = scenario.text(R.id.filterFlagged)
                assertTrue("l'onglet « signalées » affiche leur nombre : $label", Regex("\\([1-9]\\d*\\)").containsMatchIn(label))

                scenario.click(R.id.filterFlagged)
                scenario.awaitUntil { scenario.itemCount(R.id.appList) < all }
                val flagged = scenario.itemCount(R.id.appList)
                assertTrue("seules les apps signalées restent", flagged in 1 until all)

                scenario.click(R.id.filterUnflagged)
                scenario.awaitUntil { scenario.itemCount(R.id.appList) == all - flagged }
                assertEquals("signalées + non signalées = toutes", all, flagged + scenario.itemCount(R.id.appList))

                // Scrolled down in the unflagged apps, then back to "All": the flagged apps come first, so rows are inserted
                // above the scroll position; the list must still start from the top.
                scenario.awaitUntil { scenario.firstVisible() == 0 }
                scenario.onActivity {
                    val list = it.findViewById<RecyclerView>(R.id.appList)
                    list.scrollToPosition(list.adapter!!.itemCount - 1)
                }
                scenario.awaitUntil { scenario.firstVisible() > 0 }
                assertTrue("the list is scrolled down", scenario.firstVisible() > 0)

                scenario.click(R.id.filterAll)
                scenario.awaitUntil { scenario.itemCount(R.id.appList) == all && scenario.firstVisible() == 0 }
                assertEquals("toutes les apps reviennent", all, scenario.itemCount(R.id.appList))
                assertEquals("back at the top of the list", 0, scenario.firstVisible())
            }
        } finally {
            settings.setHideSystemApps(hideBefore)
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
