package app.redflag.ui

import android.app.Instrumentation
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.data.loadFaq
import app.redflag.ui.settings.FaqActivity
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The FAQ screen, really displayed (the drawer entry that opens it is checked in HomeScreenTest). */
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

    private fun ActivityScenario<FaqActivity>.textOfRow(position: Int, id: Int): String {
        var text = ""
        onActivity {
            val holder = it.findViewById<RecyclerView>(R.id.list).findViewHolderForAdapterPosition(position)!!
            text = holder.itemView.findViewById<TextView>(id).text.toString()
        }
        return text
    }

    /** One scenario on one screen: closed themes, a theme then a question opened, a rotation, then everything closed again. */
    @Test
    fun themesAndQuestionsOpenCloseAndStayOpenAfterRotation() {
        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            scenario.awaitCount(faq.size)
            assertEquals("un thème par ligne, tous fermés", faq.size, scenario.itemCount())

            val questions = faq.first().entries.size
            scenario.tapRow(0)
            scenario.awaitCount(faq.size + questions)
            assertEquals("les questions du thème apparaissent", faq.size + questions, scenario.itemCount())

            scenario.tapRow(1)
            val open = faq.size + questions + 1
            scenario.awaitCount(open)
            assertEquals("la réponse apparaît sous la question", open, scenario.itemCount())
            assertEquals(faq.first().entries.first().answer, scenario.textOfRow(2, R.id.answer))

            scenario.recreate()
            scenario.awaitCount(open)
            assertEquals("après une rotation, la question reste ouverte", open, scenario.itemCount())

            scenario.tapRow(1)
            scenario.awaitCount(faq.size + questions)
            scenario.tapRow(0)
            scenario.awaitCount(faq.size)
            assertEquals("tout se referme", faq.size, scenario.itemCount())
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
                assertEquals("L’application", scenario.textOfRow(0, R.id.themeTitle))
            }
        } finally {
            setAppLanguage(null)
        }
    }
}
