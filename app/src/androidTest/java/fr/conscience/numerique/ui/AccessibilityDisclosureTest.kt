package fr.conscience.numerique.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.DialogInterface
import android.content.IntentFilter
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.service.isFrictionServiceEnabled
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Avant d'ouvrir les réglages d'accessibilité, une fenêtre explique ce que lit le service et demande un consentement explicite. */
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
        // Surveille l'ouverture des réglages d'Android (elle ne doit jamais précéder le consentement) et la bloque : on compte les
        // tentatives sans réellement quitter l'application.
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

    @Test
    fun theSettingsRowAsksBeforeOpeningAndroidSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapPermissionsRow()

            assertNotNull("l'avertissement s'affiche", scenario.disclosure())
            assertEquals("les réglages d'Android ne sont pas encore ouverts", 0, monitor.hits)
        }
    }

    @Test
    fun theBannerOfTheHomeScreenAsksBeforeOpeningAndroidSettings() {
        assumeTrue("le bandeau n'existe que si le service est désactivé", !isFrictionServiceEnabled(context))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.findViewById<View>(R.id.enableService).performClick() }

            assertNotNull("l'avertissement s'affiche", scenario.disclosure())
            assertEquals(0, monitor.hits)
        }
    }

    @Test
    fun theDisclosureNamesWhatIsReadAndWhatIsNotShared() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapPermissionsRow()
            val dialog = scenario.disclosure()
            assertNotNull(dialog)

            var text = ""
            scenario.onActivity { text = (dialog!!.dialog as AlertDialog).findViewById<TextView>(android.R.id.message)!!.text.toString() }

            assertTrue("dit ce qui est lu : le nom de l'app au premier plan", text.contains("foreground", true) || text.contains("premier plan", true))
            assertTrue("dit que rien ne quitte l'appareil", text.contains("leaves your device") || text.contains("ne quitte"))
        }
    }

    @Test
    fun cancellingDoesNotOpenAndroidSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapPermissionsRow()
            assertNotNull(scenario.disclosure())

            scenario.press(DialogInterface.BUTTON_NEGATIVE)
            Thread.sleep(500)

            assertEquals(0, monitor.hits)
        }
    }

    @Test
    fun agreeingOpensAndroidSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapPermissionsRow()
            assertNotNull(scenario.disclosure())

            scenario.press(DialogInterface.BUTTON_POSITIVE)

            val deadline = System.currentTimeMillis() + 5_000
            while (monitor.hits == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)

            assertEquals("« Accepter et continuer » ouvre les réglages d'accessibilité", 1, monitor.hits)
        }
    }
}
