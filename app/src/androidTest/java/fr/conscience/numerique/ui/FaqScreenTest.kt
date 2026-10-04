package fr.conscience.numerique.ui

import android.app.Instrumentation
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.R
import fr.conscience.numerique.data.loadFaq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** L'écran de la FAQ, affiché pour de bon, et son accès depuis les Réglages (qui n'ouvre plus de fenêtre de dialogue). */
@RunWith(AndroidJUnit4::class)
class FaqScreenTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val faq = instrumentation.targetContext.loadFaq()

    private fun ActivityScenario<FaqActivity>.itemCount(): Int {
        var count = -1
        onActivity { count = it.findViewById<RecyclerView>(R.id.list).adapter?.itemCount ?: -1 }
        return count
    }

    /** Touche la ligne [position] de la liste, comme le ferait un doigt. */
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


    /** Change la langue de l'app comme le fait Android (réglages), depuis un test : `adb shell cmd locale set-app-locales`. */
    private fun setAppLanguage(tag: String?) {
        val args = if (tag == null) "" else " --locales $tag"
        val output = instrumentation.uiAutomation
            .executeShellCommand("cmd locale set-app-locales ${instrumentation.targetContext.packageName}$args")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        Thread.sleep(1_000) // le système applique la langue de façon asynchrone
    }

    @Test
    fun theFaqFollowsTheLanguageChosenForTheApp() {
        // Même process, langue de l'app changée : la FAQ doit se lire dans cette langue (et pas celle de l'application au démarrage).
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
    fun theHelpRowOfTheSettingsOpensTheFaqScreen() {
        val monitor = instrumentation.addMonitor(FaqActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                scenario.onActivity { it.findViewById<View>(R.id.rowHelp).performClick() }

                val opened = monitor.waitForActivityWithTimeout(5_000)

                assertNotNull("la ligne « Comment ça marche » ouvre l'écran de la FAQ", opened)
                assertTrue(opened is FaqActivity)
                instrumentation.runOnMainSync { opened.finish() }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
