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
    private val fomo = ProblemRef.catalog("fomo")
    private val sexism = ProblemRef.catalog("sexism")

    @Before
    fun setUp() = runBlocking {
        repository.linkProblem(appA, "Application A", fomo)
        repository.linkProblem(appB, "Application B", sexism)
        repository.linkProblem(appB, "Application B", ProblemRef.catalog("racism"))
    }

    @After
    fun tearDown() = runBlocking {
        repository.unlinkProblem(appA, fomo)
        repository.unlinkProblem(appB, sexism)
        repository.unlinkProblem(appB, ProblemRef.catalog("racism"))
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

    private fun answers(pkg: String) = runBlocking { repository.choiceEvents.first() }.count { it.packageName == pkg }

    /** Attend (au plus 3 s) que [pkg] ait au moins [expected] réponses enregistrées : l'écriture est asynchrone. */
    private fun awaitAnswers(pkg: String, expected: Int) {
        val deadline = System.currentTimeMillis() + 3_000
        while (answers(pkg) < expected && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    /**
     * Quitter l'écran avec le bouton Accueil : il passe à l'arrière-plan (onStop) sans qu'aucune réponse n'ait été donnée.
     * On appelle onStop directement : déplacer l'écran vers l'état « créé » avec ActivityScenario prend près d'une minute.
     */
    private fun ActivityScenario<InterstitialActivity>.leaveWithHome() =
        onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnStop(it) }

    /** Revenir sur l'écran (onStart), par exemple depuis les applications récentes. */
    private fun ActivityScenario<InterstitialActivity>.comeBack() =
        onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnStart(it) }

    @Test
    fun eachDisplayLeftWithoutAnsweringCountsOneRefusal() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            val before = answers(appA)

            scenario.leaveWithHome()
            awaitAnswers(appA, before + 1)
            assertEquals("la sortie est comptée comme un refus", before + 1, answers(appA))

            scenario.leaveWithHome()
            Thread.sleep(300)
            assertEquals("une seule fois par affichage", before + 1, answers(appA))

            scenario.comeBack()
            scenario.leaveWithHome()
            awaitAnswers(appA, before + 2)
            assertEquals("deux affichages, deux refus", before + 2, answers(appA))
        }
    }

    @Test
    fun leavingAfterAnsweringYesDoesNotAddARefusal() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            val before = answers(appA)

            scenario.onActivity { it.findViewById<android.view.View>(R.id.btnContinue).performClick() }
            Thread.sleep(500)

            val events = runBlocking { repository.choiceEvents.first() }.filter { it.packageName == appA }
            assertEquals("une seule réponse enregistrée", before + 1, events.size)
            assertTrue("c'est un Oui", events.last().proceeded)
        }
    }

    @Test
    fun rotatingTheScreenIsNotARefusal() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            val before = answers(appA)

            scenario.recreate()
            Thread.sleep(500)

            assertEquals("recréer l'écran ne compte pas", before, answers(appA))
        }
    }

    @Test
    fun leavingCountsForTheAppShownAtThatMoment() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appB))
            scenario.waitForAppName("Application B")
            val beforeA = answers(appA)
            val beforeB = answers(appB)

            scenario.leaveWithHome()
            awaitAnswers(appB, beforeB + 1)

            assertEquals("A n'est pas concernée", beforeA, answers(appA))
            assertEquals("B reçoit le refus", beforeB + 1, answers(appB))
        }
    }

    @Test
    fun showsTheAppAndFollowsTheAppItIsReusedFor() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            assertEquals("Application A", scenario.waitForAppName("Application A"))
            assertTrue(scenario.text(R.id.btnContinue).contains("Application A"))

            // La même app qui revient : rien ne change.
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appA))
            assertEquals("Application A", scenario.waitForAppName("Application A"))

            // Une autre app à interrompre passe au premier plan : Android réutilise l'écran existant.
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appB))
            assertEquals("Application B", scenario.waitForAppName("Application B"))
            assertTrue("le bouton « Oui » cite la nouvelle app", scenario.text(R.id.btnContinue).contains("Application B"))
        }
    }

    @Test
    fun answeringNoRecordsARefusalForTheCurrentApp() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appB))
            scenario.waitForAppName("Application B")

            val beforeA = answers(appA)
            val beforeB = answers(appB)
            scenario.onActivity { it.findViewById<android.view.View>(R.id.btnBack).performClick() }
            awaitAnswers(appB, beforeB + 1)
            Thread.sleep(300)

            val refusalsB = runBlocking { repository.choiceEvents.first() }.filter { it.packageName == appB }.takeLast(1)
            assertEquals("le refus est enregistré pour l'app affichée (B)", beforeB + 1, answers(appB))
            assertTrue("c'est bien un refus", refusalsB.single().let { !it.proceeded })
            assertEquals("rien pour la première app (A)", beforeA, answers(appA))
        }
    }
}
