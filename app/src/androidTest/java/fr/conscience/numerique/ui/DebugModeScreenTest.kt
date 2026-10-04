package fr.conscience.numerique.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.data.DebugUnlock
import fr.conscience.numerique.ui.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** La section « Debug » des Réglages : cachée, puis visible après sept appuis sur le numéro de version, et conservée. */
@RunWith(AndroidJUnit4::class)
class DebugModeScreenTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val settings get() = context.container.settings
    private var debugBefore = false

    @Before
    fun setUp() {
        debugBefore = settings.debugMode.value
        settings.disableDebugMode()
    }

    @After
    fun tearDown() {
        if (debugBefore) settings.enableDebugMode() else settings.disableDebugMode()
    }

    private fun ActivityScenario<SettingsActivity>.debugVisibility(): Int {
        var value = -1
        onActivity { value = it.findViewById<View>(R.id.debugGroup).visibility }
        return value
    }

    private fun ActivityScenario<SettingsActivity>.tapVersion(times: Int) {
        repeat(times) { onActivity { it.findViewById<View>(R.id.rowVersion).performClick() } }
    }

    private fun ActivityScenario<SettingsActivity>.awaitVisibility(expected: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (debugVisibility() != expected && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Test
    fun theDebugSectionIsHiddenAtFirst() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            assertEquals(View.GONE, scenario.debugVisibility())
        }
    }

    @Test
    fun sixTapsOnTheVersionAreNotEnough() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapVersion(DebugUnlock.TAPS_REQUIRED - 1)
            Thread.sleep(300)

            assertEquals(View.GONE, scenario.debugVisibility())
            assertFalse(settings.debugMode.value)
        }
    }

    @Test
    fun sevenTapsShowTheDebugSectionAndItIsKept() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapVersion(DebugUnlock.TAPS_REQUIRED)
            scenario.awaitVisibility(View.VISIBLE)

            assertEquals(View.VISIBLE, scenario.debugVisibility())
            assertTrue("le mode est enregistré", settings.debugMode.value)
        }

        // De retour sur l'écran (ou après un redémarrage de l'app), la section est toujours là sans refaire les appuis.
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.awaitVisibility(View.VISIBLE)
            assertEquals(View.VISIBLE, scenario.debugVisibility())
        }
    }
}
