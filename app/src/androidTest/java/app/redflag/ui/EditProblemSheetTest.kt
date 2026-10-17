package app.redflag.ui

import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.ui.common.EditProblemSheet
import app.redflag.ui.settings.FaqActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The "Edit issue" bottom sheet: a long title must be readable in full (it wraps over several lines). */
@RunWith(AndroidJUnit4::class)
class EditProblemSheetTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun ActivityScenario<FaqActivity>.showEditDialog(current: String, catalog: Boolean): EditText {
        onActivity { EditProblemSheet.show(it, current = current, catalog = catalog, canReset = false) }
        var input: EditText? = null
        val deadline = System.currentTimeMillis() + 5_000
        while (input == null && System.currentTimeMillis() < deadline) {
            onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                input = activity.supportFragmentManager.fragments.filterIsInstance<EditProblemSheet>().firstOrNull()?.view?.findViewById(R.id.problemLabel)
            }
            if (input == null) Thread.sleep(50)
        }
        return checkNotNull(input) { "la fenêtre ne s'est pas ouverte" }
    }

    /** Waits until the field is laid out (the number of lines only exists then). */
    private fun ActivityScenario<FaqActivity>.lines(input: EditText): Int {
        // The sheet slides in: wait until the field has its final width and its line count stops changing.
        var count = 0
        var stable = 0
        val deadline = System.currentTimeMillis() + 5_000
        while (stable < 5 && System.currentTimeMillis() < deadline) {
            var now = 0
            onActivity { now = if (input.isLaidOut && input.width > 0) input.lineCount else 0 }
            stable = if (now > 0 && now == count) stable + 1 else 0
            count = now
            Thread.sleep(100)
        }
        return count
    }

    private fun ActivityScenario<FaqActivity>.closeSheet() = onActivity { activity ->
        activity.supportFragmentManager.fragments.filterIsInstance<EditProblemSheet>().forEach { it.dismissNow() }
    }

    /** One scenario, one screen: a long title wraps over several lines, then a short one stays on a single line. */
    @Test
    fun aLongTitleWrapsAndAShortOneStaysOnOneLine() {
        // A catalog title renamed by the user can be long: it must stay readable (the catalog titles themselves are short now).
        val label = "Worker exploitation in warehouses, delivery services and content moderation farms"
        assertTrue("l'intitulé de ce test est bien long", label.length > 60)

        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            val long = scenario.showEditDialog(label, catalog = true)
            assertEquals("le texte n'est pas coupé", label, long.text.toString())
            assertTrue("il passe à la ligne au lieu de défiler sur une seule", scenario.lines(long) >= 2)

            scenario.closeSheet()
            val short = scenario.showEditDialog("Trop de pubs", catalog = false)
            assertEquals("un titre court tient sur une ligne", 1, scenario.lines(short))
        }
    }
}
