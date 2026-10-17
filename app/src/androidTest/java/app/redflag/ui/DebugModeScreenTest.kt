package app.redflag.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.data.DebugUnlock
import app.redflag.ui.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The "Debug" section of the Settings: hidden, then visible after seven taps on the version number, and kept. */
@RunWith(AndroidJUnit4::class)
class DebugModeScreenTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val settings get() = context.container.settings
    private var debugBefore = false
    private var onboardingBefore = false

    @Before
    fun setUp() {
        debugBefore = settings.debugMode.value
        onboardingBefore = settings.onboardingDone
        settings.onboardingDone = true // the home screen (with the drawer) must not send to the welcome tour
        settings.disableDebugMode()
    }

    @After
    fun tearDown() {
        settings.onboardingDone = onboardingBefore
        if (debugBefore) settings.enableDebugMode() else settings.disableDebugMode()
    }

    private fun ActivityScenario<SettingsActivity>.debugVisibility(): Int {
        var value = -1
        onActivity { value = it.findViewById<View>(R.id.debugGroup).visibility }
        return value
    }

    /** The version is in the drawer of the home screen: the taps are given there, before the Settings are looked at. */
    private fun tapVersion(times: Int) {
        ActivityScenario.launch(app.redflag.ui.home.HomeActivity::class.java).use { home ->
            repeat(times) { home.onActivity { it.findViewById<View>(R.id.drawerVersion).performClick() } }
        }
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
        tapVersion(DebugUnlock.TAPS_REQUIRED - 1)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->

            assertEquals(View.GONE, scenario.debugVisibility())
            assertFalse(settings.debugMode.value)
        }
    }

    @Test
    fun sevenTapsShowTheDebugSectionAndItIsKept() {
        tapVersion(DebugUnlock.TAPS_REQUIRED)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.awaitVisibility(View.VISIBLE)

            assertEquals(View.VISIBLE, scenario.debugVisibility())
            assertTrue("le mode est enregistré", settings.debugMode.value)
        }

        // Back on the screen (or after a restart of the app), the section is still there without tapping again.
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.awaitVisibility(View.VISIBLE)
            assertEquals(View.VISIBLE, scenario.debugVisibility())
        }
    }
}
