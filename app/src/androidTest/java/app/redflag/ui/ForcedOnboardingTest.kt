package app.redflag.ui

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.container
import app.redflag.ui.home.HomeActivity
import app.redflag.ui.onboarding.OnboardingActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The debug setting "Always show the welcome tour": the tour shows at every launch from the app icon, even when the process
 * stays alive (the accessibility service keeps it running, which had made the setting useless).
 */
@RunWith(AndroidJUnit4::class)
class ForcedOnboardingTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val settings get() = context.container.settings
    private var doneBefore = false
    private var alwaysBefore = false
    private var debugBefore = false

    @Before
    fun setUp() {
        doneBefore = settings.onboardingDone
        debugBefore = settings.debugMode.value
        settings.enableDebugMode() // the "always show the welcome tour" setting only exists in debug mode
        alwaysBefore = settings.alwaysShowOnboarding.value
        settings.onboardingDone = true
    }

    @After
    fun tearDown() {
        settings.setAlwaysShowOnboarding(alwaysBefore)
        settings.onboardingDone = doneBefore
        if (!debugBefore) settings.disableDebugMode()
    }

    private fun launcherIntent() = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(context, HomeActivity::class.java))

    /** Launches the app and tells whether the welcome tour opened. */
    private fun onboardingOpensFor(intent: Intent): Boolean {
        val monitor = instrumentation.addMonitor(OnboardingActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<HomeActivity>(intent).use {
                val opened = monitor.waitForActivityWithTimeout(2_000)
                opened?.let { activity -> instrumentation.runOnMainSync { activity.finish() } }
                return opened != null
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    /**
     * One scenario: setting off, the icon opens no welcome tour; setting on, every launch from the icon shows it (twice in a row in
     * the same process); and coming back to the home screen from a tab does not restart it.
     */
    @Test
    fun theSettingDecidesWhetherEveryLaunchFromTheIconShowsTheOnboarding() {
        settings.setAlwaysShowOnboarding(false)
        assertFalse("réglage éteint : pas d'accueil", onboardingOpensFor(launcherIntent()))

        settings.setAlwaysShowOnboarding(true)
        assertTrue("réglage allumé : l'accueil s'affiche", onboardingOpensFor(launcherIntent()))
        assertTrue("et encore au lancement suivant, dans le même processus", onboardingOpensFor(launcherIntent()))

        // The tabs open the home screen again with an intent without the launcher category: no welcome tour.
        assertFalse("depuis les onglets : pas d'accueil", onboardingOpensFor(Intent(context, HomeActivity::class.java)))
    }
}
