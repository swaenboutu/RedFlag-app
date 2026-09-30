package fr.conscience.numerique.ui

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.data.ProblemRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'écran d'interruption est en `singleTask` : quand il est déjà ouvert pour une app et qu'une autre app à interrompre
 * passe au premier plan, Android le réutilise. Il doit alors se mettre à jour (bug déjà rencontré : il restait sur la première app).
 */
@RunWith(AndroidJUnit4::class)
class InterstitialActivityTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = context.container.repository

    private val appA = "test.interstitial.a"
    private val appB = "test.interstitial.b"
    private val fomo = ProblemRef(catalogKey = "fomo")
    private val sexism = ProblemRef(catalogKey = "sexism")

    @Before
    fun setUp() = runBlocking {
        repository.linkProblem(appA, "Application A", fomo)
        repository.linkProblem(appB, "Application B", sexism)
        repository.linkProblem(appB, "Application B", ProblemRef(catalogKey = "racism"))
    }

    @After
    fun tearDown() = runBlocking {
        repository.unlinkProblem(appA, fomo)
        repository.unlinkProblem(appB, sexism)
        repository.unlinkProblem(appB, ProblemRef(catalogKey = "racism"))
    }

    /** Attend (au plus 5 s) que l'écran affiche [expected] : le chargement se fait en arrière-plan. */
    private fun ActivityScenario<InterstitialActivity>.waitForAppName(expected: String): String {
        var shown = ""
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            onActivity { shown = it.findViewById<TextView>(R.id.appName).text.toString() }
            if (shown == expected) break
            Thread.sleep(50)
        }
        return shown
    }

    /** Ce que fait Android quand il réutilise l'écran : il appelle `onNewIntent` (protégé) avec le nouvel intent. */
    private fun ActivityScenario<InterstitialActivity>.deliverNewIntent(intent: android.content.Intent) {
        onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, intent) }
    }

    private fun ActivityScenario<InterstitialActivity>.text(id: Int): String {
        var text = ""
        onActivity { text = it.findViewById<TextView>(id).text.toString() }
        return text
    }

    @Test
    fun showsTheAppAndItsProblems() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            assertEquals("Application A", scenario.waitForAppName("Application A"))
            assertTrue(scenario.text(R.id.btnContinue).contains("Application A"))
        }
    }

    @Test
    fun updatesWhenReusedForAnotherApp() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            assertEquals("Application A", scenario.waitForAppName("Application A"))

            // Une autre app à interrompre passe au premier plan : Android réutilise l'écran existant.
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appB))

            assertEquals("Application B", scenario.waitForAppName("Application B"))
            assertTrue("le bouton « Oui » cite la nouvelle app", scenario.text(R.id.btnContinue).contains("Application B"))
        }
    }

    @Test
    fun keepsTheSameAppWhenReusedForTheSameApp() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            assertEquals("Application A", scenario.waitForAppName("Application A"))

            scenario.deliverNewIntent(InterstitialActivity.intent(context, appA))

            assertEquals("Application A", scenario.waitForAppName("Application A"))
        }
    }

    @Test
    fun answeringNoRecordsARefusalForTheCurrentApp() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appB))
            scenario.waitForAppName("Application B")

            val before = runBlocking { repository.choiceEvents.first() }.count { it.packageName == appB && !it.proceeded }
            scenario.onActivity { it.findViewById<android.view.View>(R.id.btnBack).performClick() }
            Thread.sleep(500)

            val after = runBlocking { repository.choiceEvents.first() }.filter { it.packageName == appB && !it.proceeded }
            assertEquals("le refus est enregistré pour l'app affichée (B), pas pour la première (A)", before + 1, after.size)
            assertEquals(0, runBlocking { repository.choiceEvents.first() }.count { it.packageName == appA && !it.proceeded })
        }
    }
}
