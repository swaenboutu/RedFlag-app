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
 * Les écrans de détail (fiche d'une app, statistiques d'une app) restent ouverts : ils portent la barre de navigation, et
 * remettre l'onglet en surbrillance au retour sur l'écran ne doit surtout pas les refermer (régression déjà rencontrée).
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
        // Laisse le temps à un éventuel finish() déclenché au retour sur l'écran (onResume).
        Thread.sleep(1_000)
        return state
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
                Thread.sleep(500)
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
