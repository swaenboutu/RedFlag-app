package app.redflag.ui

import android.app.Instrumentation
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.data.loadFaq
import app.redflag.ui.settings.FaqActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The FAQ screen, really displayed, and how the drawer opens it. */
@RunWith(AndroidJUnit4::class)
class FaqScreenTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val faq = instrumentation.targetContext.loadFaq()

    private fun ActivityScenario<FaqActivity>.itemCount(): Int {
        var count = -1
        onActivity { count = it.findViewById<RecyclerView>(R.id.list).adapter?.itemCount ?: -1 }
        return count
    }

    /** Taps row [position] of the list, as a finger would. */
    private fun ActivityScenario<FaqActivity>.tapRow(position: Int) {
        onActivity { it.findViewById<RecyclerView>(R.id.list).findViewHolderForAdapterPosition(position)!!.itemView.performClick() }
    }

    private fun ActivityScenario<FaqActivity>.awaitCount(expected: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (itemCount() != expected && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Test
    fun allThemesAreListedAndClosedAtFirst() {
        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            scenario.awaitCount(faq.size)

            assertEquals("un thème par ligne, tous fermés", faq.size, scenario.itemCount())
        }
    }

    @Test
    fun tappingAThemeThenAQuestionRevealsTheQuestionsThenTheAnswer() {
        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            scenario.awaitCount(faq.size)
            val questions = faq.first().entries.size

            scenario.tapRow(0)
            scenario.awaitCount(faq.size + questions)
            assertEquals("les questions du thème apparaissent", faq.size + questions, scenario.itemCount())

            scenario.tapRow(1)
            scenario.awaitCount(faq.size + questions + 1)
            assertEquals("la réponse apparaît sous la question", faq.size + questions + 1, scenario.itemCount())

            var answer = ""
            scenario.onActivity {
                val holder = it.findViewById<RecyclerView>(R.id.list).findViewHolderForAdapterPosition(2)!!
                answer = holder.itemView.findViewById<TextView>(R.id.answer).text.toString()
            }
            assertEquals(faq.first().entries.first().answer, answer)

            scenario.tapRow(1)
            scenario.awaitCount(faq.size + questions)
            scenario.tapRow(0)
            scenario.awaitCount(faq.size)
            assertEquals("tout se referme", faq.size, scenario.itemCount())
        }
    }

    @Test
    fun anOpenQuestionStaysOpenAfterRotation() {
        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            scenario.awaitCount(faq.size)
            scenario.tapRow(0)
            scenario.awaitCount(faq.size + faq.first().entries.size)
            scenario.tapRow(1)
            val expected = faq.size + faq.first().entries.size + 1
            scenario.awaitCount(expected)

            scenario.recreate()

            scenario.awaitCount(expected)
            assertEquals(expected, scenario.itemCount())
        }
    }


    /** Changes the language of the app as Android does (settings), from a test: `adb shell cmd locale set-app-locales`. */
    private fun setAppLanguage(tag: String?) {
        val args = if (tag == null) "" else " --locales $tag"
        val output = instrumentation.uiAutomation
            .executeShellCommand("cmd locale set-app-locales ${instrumentation.targetContext.packageName}$args")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        // The system applies the language asynchronously: wait (at most 5 s) until it reports the new one for the app.
        val locales = instrumentation.targetContext.getSystemService(android.app.LocaleManager::class.java)
        val deadline = System.currentTimeMillis() + 5_000
        while (locales.applicationLocales.toLanguageTags() != (tag ?: "") && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Test
    fun theFaqFollowsTheLanguageChosenForTheApp() {
        // The `cmd locale` command (per-app language, from the shell) only exists since Android 13.
        assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        // Same process, language of the app changed: the FAQ must read in that language (not the one the app started with).
        setAppLanguage("fr-FR")
        try {
            ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
                scenario.awaitCount(faq.size)
                var title = ""
                scenario.onActivity {
                    val holder = it.findViewById<RecyclerView>(R.id.list).findViewHolderForAdapterPosition(0)!!
                    title = holder.itemView.findViewById<TextView>(R.id.themeTitle).text.toString()
                }

                assertEquals("L’application", title)
            }
        } finally {
            setAppLanguage(null)
        }
    }

    @Test
    fun theDrawerOpensTheFaqScreen() {
        val settings = instrumentation.targetContext.container.settings
        val onboardingBefore = settings.onboardingDone
        settings.onboardingDone = true // the home screen must not send to the welcome tour
        val monitor = instrumentation.addMonitor(FaqActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(app.redflag.ui.home.HomeActivity::class.java).use { scenario ->
                scenario.onActivity { it.findViewById<View>(R.id.drawerFaq).performClick() }

                val opened = monitor.waitForActivityWithTimeout(5_000)

                assertNotNull("the drawer entry opens the FAQ screen", opened)
                assertTrue(opened is FaqActivity)
                instrumentation.runOnMainSync { opened.finish() }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            settings.onboardingDone = onboardingBefore
        }
    }
}
