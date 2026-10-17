package app.redflag.ui

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.data.ProblemRef
import app.redflag.ui.interruption.InterstitialActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The interruption screen is `singleTask`: when it is already open for an app and another app to interrupt comes to the
 * foreground, Android reuses it. It must then update itself (a bug seen before: it stayed on the first app).
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

    /** Waits (at most 5 s) until the screen shows [expected]: it loads in the background. */
    private fun ActivityScenario<InterstitialActivity>.waitForAppName(expected: String): String {
        var shown = ""
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            onActivity { shown = it.findViewById<TextView>(R.id.subtitle).text.toString() }
            if (shown.contains(expected)) break
            Thread.sleep(50)
        }
        return if (shown.contains(expected)) expected else shown
    }

    /** What Android does when it reuses the screen: it calls `onNewIntent` (protected) with the new intent. */
    private fun ActivityScenario<InterstitialActivity>.deliverNewIntent(intent: android.content.Intent) {
        onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, intent) }
    }

    private fun ActivityScenario<InterstitialActivity>.text(id: Int): String {
        var text = ""
        onActivity { text = it.findViewById<TextView>(id).text.toString() }
        return text
    }

    private fun answers(pkg: String) = runBlocking { repository.choiceEvents.first() }.count { it.packageName == pkg }

    /** Waits (at most 3 s) until [pkg] has at least [expected] answers recorded: the write is asynchronous. */
    private fun awaitAnswers(pkg: String, expected: Int) {
        val deadline = System.currentTimeMillis() + 3_000
        while (answers(pkg) < expected && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    /**
     * Leaving the screen with the Home button: it goes to the background (onStop) without any answer given.
     * onStop is called directly: moving the screen to the "created" state with ActivityScenario takes almost a minute.
     */
    private fun ActivityScenario<InterstitialActivity>.leaveWithHome() =
        onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnStop(it) }

    /** Coming back to the screen (onStart), for example from the recent apps. */
    private fun ActivityScenario<InterstitialActivity>.comeBack() =
        onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnStart(it) }

    /**
     * One scenario on one screen, as a user would live it: the screen shows the app (and stays on it when the same app comes back),
     * a rotation is not a refusal, leaving without answering counts one refusal per display, a reused screen follows the new app
     * (all its issues listed) and its refusals go to that app, and "No" records a refusal for the app shown.
     */
    @Test
    fun theScreenFollowsTheAppShownAndCountsOneRefusalPerDisplay() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            assertEquals("Application A", scenario.waitForAppName("Application A"))
            assertTrue(scenario.text(R.id.subtitle).contains("Application A"))

            // The same app coming back: nothing changes.
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appA))
            assertEquals("Application A", scenario.waitForAppName("Application A"))

            // A rotation recreates the screen: it is not a refusal.
            var before = answers(appA)
            scenario.recreate()
            scenario.waitForAppName("Application A")
            Thread.sleep(300) // nothing must be written: give a wrong write the time to happen
            assertEquals("recréer l'écran ne compte pas", before, answers(appA))

            // Leaving with the Home button: one refusal per display, not one per exit.
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

            // Another app to interrupt comes to the foreground: Android reuses the screen, which follows the new app.
            scenario.comeBack()
            scenario.deliverNewIntent(InterstitialActivity.intent(context, appB))
            assertEquals("Application B", scenario.waitForAppName("Application B"))
            assertTrue("le sous-titre cite la nouvelle app", scenario.text(R.id.subtitle).contains("Application B"))
            val problems = scenario.text(R.id.problems)
            val sexismLabel = context.getString(R.string.problem_sexism)
            val racismLabel = context.getString(R.string.problem_racism)
            assertTrue("les deux problématiques sont listées", problems.contains(sexismLabel) && problems.contains(racismLabel))

            before = answers(appA)
            var beforeB = answers(appB)
            scenario.leaveWithHome()
            awaitAnswers(appB, beforeB + 1)
            assertEquals("A n'est pas concernée", before, answers(appA))
            assertEquals("B reçoit le refus", beforeB + 1, answers(appB))

            // "No": a refusal for the app shown (B), nothing for the first one.
            scenario.comeBack()
            beforeB = answers(appB)
            scenario.onActivity { it.findViewById<android.view.View>(R.id.btnBack).performClick() }
            awaitAnswers(appB, beforeB + 1)
            Thread.sleep(300)
            val refusalsB = runBlocking { repository.choiceEvents.first() }.filter { it.packageName == appB }.takeLast(1)
            assertEquals("le refus est enregistré pour l'app affichée (B)", beforeB + 1, answers(appB))
            assertTrue("c'est bien un refus", refusalsB.single().let { !it.proceeded })
            assertEquals("rien pour la première app (A)", before, answers(appA))
        }
    }

    @Test
    fun leavingAfterAnsweringYesDoesNotAddARefusal() {
        ActivityScenario.launch<InterstitialActivity>(InterstitialActivity.intent(context, appA)).use { scenario ->
            scenario.waitForAppName("Application A")
            val before = answers(appA)

            scenario.onActivity { it.findViewById<android.view.View>(R.id.btnContinue).performClick() }
            awaitAnswers(appA, before + 1)
            Thread.sleep(300) // a refusal written on leaving would come right after

            val events = runBlocking { repository.choiceEvents.first() }.filter { it.packageName == appA }
            assertEquals("une seule réponse enregistrée", before + 1, events.size)
            assertTrue("c'est un Oui", events.last().proceeded)
        }
    }
}
