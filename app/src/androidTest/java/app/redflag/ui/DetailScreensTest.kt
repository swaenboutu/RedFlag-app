package app.redflag.ui

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.container
import app.redflag.data.ProblemRef
import app.redflag.ui.apps.AppDetailActivity
import app.redflag.ui.problems.ProblemDetailActivity
import app.redflag.ui.stats.AppStatsActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The detail screens (an app's page, an app's statistics) stay open: they carry the navigation bar, and highlighting the tab
 * again when coming back to the screen must never close them (a regression seen before).
 */
@RunWith(AndroidJUnit4::class)
class DetailScreensTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = context.container.repository
    private val pkg = "test.detail.screens"
    private val ref = ProblemRef.catalog("fomo")

    @Before
    fun setUp() = runBlocking { repository.linkProblem(pkg, "Application détail", ref) }

    @After
    fun tearDown() = runBlocking { repository.unlinkProblem(pkg, ref) }

    private fun <A : androidx.appcompat.app.AppCompatActivity> ActivityScenario<A>.staysResumed(): Lifecycle.State {
        // A finish() triggered when the screen comes back (onResume) runs on the main thread: once it is idle, the screen is
        // either still resumed or already finishing.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        if (state != Lifecycle.State.RESUMED) return state
        var finishing = false
        onActivity { finishing = it.isFinishing }
        return if (finishing) Lifecycle.State.DESTROYED else state
    }

    @Test
    fun appStatisticsStayOpenEvenAfterBeingRecreated() {
        ActivityScenario.launch<AppStatsActivity>(AppStatsActivity.intent(context, pkg, "Application détail")).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.staysResumed())
            scenario.recreate()
            assertEquals(Lifecycle.State.RESUMED, scenario.staysResumed())
        }
    }

    @Test
    fun appDetailAndProblemDetailStayOpen() {
        ActivityScenario.launch<AppDetailActivity>(AppDetailActivity.intent(context, pkg, "Application détail")).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.staysResumed())
        }
        ActivityScenario.launch<ProblemDetailActivity>(ProblemDetailActivity.intent(context, ref)).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.staysResumed())
        }
    }

    @Test
    fun aLongListOfLinkedAppsIsShownInFullAndScrollsWithThePage() {
        val extra = (1..30).map { "test.detail.many$it" }
        runBlocking { extra.forEach { repository.linkProblem(it, "Many $it", ref) } }
        try {
            ActivityScenario.launch<ProblemDetailActivity>(ProblemDetailActivity.intent(context, ref)).use { scenario ->
                var rows = 0
                var listHeight = 0
                val screen = context.resources.displayMetrics.heightPixels
                val deadline = System.currentTimeMillis() + 5_000
                while (rows < extra.size && System.currentTimeMillis() < deadline) {
                    scenario.onActivity {
                        val list = it.findViewById<android.widget.LinearLayout>(app.redflag.R.id.linkedApps)
                        rows = list.childCount
                        listHeight = list.height
                    }
                    if (rows < extra.size) Thread.sleep(100)
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync() // the rows are laid out
                scenario.onActivity { listHeight = it.findViewById<android.view.View>(app.redflag.R.id.linkedApps).height }

                assert(rows >= extra.size + 1) { "une ligne par app liée : $rows" }
                // A list clamped to the screen would be shorter than the screen: the page must be taller and scroll.
                assert(listHeight > screen) { "la liste ($listHeight px) est plus haute que l'écran ($screen px)" }
            }
        } finally {
            runBlocking { extra.forEach { repository.unlinkProblem(it, ref) } }
        }
    }
}
