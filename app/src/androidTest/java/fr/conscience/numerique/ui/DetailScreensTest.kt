package fr.conscience.numerique.ui

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.container
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.ui.apps.AppDetailActivity
import fr.conscience.numerique.ui.problems.ProblemDetailActivity
import fr.conscience.numerique.ui.stats.AppStatsActivity
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
}
