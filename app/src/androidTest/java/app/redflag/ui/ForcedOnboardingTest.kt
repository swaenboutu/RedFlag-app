package app.redflag.ui

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.container
import app.redflag.ui.apps.MainActivity
import app.redflag.ui.onboarding.OnboardingActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Le réglage de debug « Toujours afficher l'accueil » : l'accueil s'affiche à chaque lancement depuis l'icône de l'app, même
 * quand le processus reste en vie (le service d'accessibilité l'empêche de s'arrêter, c'est ce qui avait rendu le réglage inopérant).
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
        settings.enableDebugMode() // le réglage « toujours afficher l'accueil » n'existe qu'en mode debug
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
        .setComponent(ComponentName(context, MainActivity::class.java))

    /** Lance l'app et dit si l'écran d'accueil s'est ouvert. */
    private fun onboardingOpensFor(intent: Intent): Boolean {
        val monitor = instrumentation.addMonitor(OnboardingActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<MainActivity>(intent).use {
                val opened = monitor.waitForActivityWithTimeout(2_000)
                opened?.let { activity -> instrumentation.runOnMainSync { activity.finish() } }
                return opened != null
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun everyLaunchFromTheIconShowsTheOnboardingWhenForced() {
        settings.setAlwaysShowOnboarding(true)

        // Deux lancements de suite dans le même processus : le second montre l'accueil aussi.
        assertTrue(onboardingOpensFor(launcherIntent()))
        assertTrue(onboardingOpensFor(launcherIntent()))
    }

    @Test
    fun theSettingOffKeepsTheOnboardingHidden() {
        settings.setAlwaysShowOnboarding(false)

        assertFalse(onboardingOpensFor(launcherIntent()))
    }

    @Test
    fun returningToTheHomeScreenFromAnotherScreenOfTheAppDoesNotRestartTheOnboarding() {
        settings.setAlwaysShowOnboarding(true)

        // Les onglets rouvrent l'écran principal par un intent sans catégorie « lanceur » : pas d'accueil.
        val fromTabs = Intent(context, MainActivity::class.java)
        assertFalse(onboardingOpensFor(fromTabs))
    }
}
