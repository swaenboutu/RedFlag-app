package app.redflag.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.ui.common.AccessibilityDisclosureDialog
import app.redflag.ui.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Before the accessibility settings open, a window explains what the service reads and asks for an explicit consent. */
@RunWith(AndroidJUnit4::class)
class AccessibilityDisclosureTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var monitor: Instrumentation.ActivityMonitor
    private var doneBefore = false

    @Before
    fun setUp() {
        doneBefore = context.container.settings.onboardingDone
        context.container.settings.onboardingDone = true
        // Watches for Android's settings being opened (it must never come before the consent) and blocks it: the attempts are
        // counted without really leaving the app.
        monitor = instrumentation.addMonitor(
            IntentFilter(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()),
            true,
        )
    }

    @After
    fun tearDown() {
        instrumentation.removeMonitor(monitor)
        context.container.settings.onboardingDone = doneBefore
    }

    private fun <A : AppCompatActivity> ActivityScenario<A>.disclosure(): AccessibilityDisclosureDialog? {
        var dialog: AccessibilityDisclosureDialog? = null
        val deadline = System.currentTimeMillis() + 5_000
        while (dialog == null && System.currentTimeMillis() < deadline) {
            onActivity {
                it.supportFragmentManager.executePendingTransactions()
                dialog = it.supportFragmentManager.fragments.filterIsInstance<AccessibilityDisclosureDialog>().firstOrNull()
            }
            if (dialog == null) Thread.sleep(50)
        }
        return dialog
    }

    private fun <A : AppCompatActivity> ActivityScenario<A>.press(button: Int) {
        onActivity { activity ->
            val dialog = activity.supportFragmentManager.fragments.filterIsInstance<AccessibilityDisclosureDialog>().first().dialog
            (dialog as AlertDialog).getButton(button).performClick()
        }
    }

    private fun ActivityScenario<SettingsActivity>.tapPermissionsRow() =
        onActivity { it.findViewById<View>(R.id.rowPermissions).performClick() }

    /**
     * One scenario on the Settings screen: the permissions row shows the warning first (Android's settings stay closed), the
     * warning says what is read and what is not shared, "Cancel" opens nothing, and agreeing opens Android's settings once.
     */
    @Test
    fun theWarningComesFirstExplainsItselfAndOnlyAgreeingOpensAndroidSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapPermissionsRow()
            val dialog = scenario.disclosure()
            assertNotNull("l'avertissement s'affiche", dialog)
            assertEquals("les réglages d'Android ne sont pas encore ouverts", 0, monitor.hits)

            var text = ""
            scenario.onActivity { text = (dialog!!.dialog as AlertDialog).findViewById<TextView>(android.R.id.message)!!.text.toString() }
            assertTrue("dit ce qui est lu : le nom de l'app au premier plan", text.contains("foreground", true) || text.contains("premier plan", true))
            assertTrue("dit que rien ne quitte l'appareil", text.contains("leaves your device") || text.contains("ne quitte"))

            scenario.press(DialogInterface.BUTTON_NEGATIVE)
            instrumentation.waitForIdleSync() // the settings would be opened right from the click
            assertEquals("« Annuler » n'ouvre rien", 0, monitor.hits)

            scenario.awaitNoDisclosure()
            scenario.tapPermissionsRow()
            assertNotNull("l'avertissement revient", scenario.disclosure())
            scenario.press(DialogInterface.BUTTON_POSITIVE)
            val deadline = System.currentTimeMillis() + 5_000
            while (monitor.hits == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertEquals("« Accepter et continuer » ouvre les réglages d'accessibilité", 1, monitor.hits)
        }
    }

    /** Waits (at most 5 s) until the cancelled warning is gone, so that the next tap shows a new one. */
    private fun <A : AppCompatActivity> ActivityScenario<A>.awaitNoDisclosure() {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            var shown = true
            onActivity {
                it.supportFragmentManager.executePendingTransactions()
                shown = it.supportFragmentManager.fragments.any { f -> f is AccessibilityDisclosureDialog }
            }
            if (!shown) return
            Thread.sleep(50)
        }
    }
}
