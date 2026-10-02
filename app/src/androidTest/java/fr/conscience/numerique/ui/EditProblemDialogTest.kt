package fr.conscience.numerique.ui

import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** La fenêtre « Modifier la problématique » : un intitulé long doit s'y lire en entier (il passe à la ligne). */
@RunWith(AndroidJUnit4::class)
class EditProblemDialogTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun ActivityScenario<FaqActivity>.showEditDialog(current: String, catalog: Boolean): EditText {
        onActivity { EditProblemDialog.show(it, current = current, catalog = catalog, canReset = false) }
        var input: EditText? = null
        val deadline = System.currentTimeMillis() + 5_000
        while (input == null && System.currentTimeMillis() < deadline) {
            onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                val dialog = activity.supportFragmentManager.fragments.filterIsInstance<EditProblemDialog>().firstOrNull()?.dialog
                input = dialog?.findViewById(R.id.problemLabel)
            }
            if (input == null) Thread.sleep(50)
        }
        return checkNotNull(input) { "la fenêtre ne s'est pas ouverte" }
    }

    /** Attend que le champ ait été mis en page (le nombre de lignes n'existe qu'ensuite). */
    private fun ActivityScenario<FaqActivity>.lines(input: EditText): Int {
        var count = 0
        val deadline = System.currentTimeMillis() + 3_000
        while (count == 0 && System.currentTimeMillis() < deadline) {
            onActivity { count = input.lineCount }
            if (count == 0) Thread.sleep(50)
        }
        return count
    }

    @Test
    fun aLongCatalogLabelIsShownInFullOnSeveralLines() {
        val label = context.getString(R.string.problem_worker_exploitation)
        assertTrue("l'intitulé de ce test est bien long", label.length > 60)

        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            val input = scenario.showEditDialog(label, catalog = true)

            assertEquals("le texte n'est pas coupé", label, input.text.toString())
            assertTrue("il passe à la ligne au lieu de défiler sur une seule", scenario.lines(input) >= 2)
        }
    }

    @Test
    fun aShortLabelStaysOnOneLine() {
        ActivityScenario.launch(FaqActivity::class.java).use { scenario ->
            val input = scenario.showEditDialog("Trop de pubs", catalog = false)

            assertNotNull(input)
            assertEquals(1, scenario.lines(input))
        }
    }
}
