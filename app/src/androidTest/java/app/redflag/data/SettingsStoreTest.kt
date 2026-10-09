package app.redflag.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Les réglages, sur de vraies préférences : valeurs par défaut d'une première utilisation, puis valeurs enregistrées. */
@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun freshInstall() {
        // Une première utilisation : aucune préférence enregistrée.
        context.deleteSharedPreferences("settings")
    }

    @Test
    fun systemAppsAreHiddenByDefault() {
        assertTrue(SettingsStore(context).hideSystemApps.value)
    }

    @Test
    fun otherDefaults() {
        val settings = SettingsStore(context)
        assertTrue(settings.interceptionEnabled.value)
        assertEquals(SettingsStore.DEFAULT_PAUSE_MINUTES, settings.pauseMinutes.value)
        assertFalse(settings.onboardingDone)
    }

    @Test
    fun aTimedDeactivationEndsByItself() {
        var time = 1_000_000L
        val settings = SettingsStore(context) { time }

        settings.disableInterceptionFor(60)
        assertFalse(settings.isInterceptionActive())
        assertEquals(1_000_000L + 60 * 60_000L, settings.reenableAt.value)

        time += 59 * 60_000L
        assertFalse("not over yet", settings.isInterceptionActive())

        time += 2 * 60_000L
        assertTrue("over: interruptions are back", settings.isInterceptionActive())
        assertTrue("and the switch is on again", settings.interceptionEnabled.value)
        assertEquals(0L, settings.reenableAt.value)
    }

    @Test
    fun aDeactivationWithoutDurationNeverEnds() {
        var time = 1_000_000L
        val settings = SettingsStore(context) { time }

        settings.setInterceptionEnabled(false)
        time += 365L * 24 * 60 * 60_000L

        assertFalse(settings.isInterceptionActive())
        assertEquals(0L, settings.reenableAt.value)
    }

    @Test
    fun turningTheSwitchBackOnForgetsTheTimer() {
        var time = 1_000_000L
        val settings = SettingsStore(context) { time }
        settings.disableInterceptionFor(60)

        settings.setInterceptionEnabled(true)

        assertTrue(settings.isInterceptionActive())
        assertEquals(0L, settings.reenableAt.value)
    }

    @Test
    fun aTimedDeactivationIsRememberedAcrossLaunches() {
        var time = 1_000_000L
        SettingsStore(context) { time }.disableInterceptionFor(12 * 60)

        val reopened = SettingsStore(context) { time }
        assertFalse(reopened.isInterceptionActive())
        time += 13 * 60 * 60_000L
        assertTrue(reopened.isInterceptionActive())
    }

    @Test
    fun showingSystemAppsIsRememberedAcrossLaunches() {
        SettingsStore(context).setHideSystemApps(false)

        assertFalse("le choix de l'utilisateur l'emporte sur la valeur par défaut", SettingsStore(context).hideSystemApps.value)
    }

    @Test
    fun hidingThemAgainIsRemembered() {
        SettingsStore(context).apply {
            setHideSystemApps(false)
            setHideSystemApps(true)
        }

        assertTrue(SettingsStore(context).hideSystemApps.value)
    }

    @Test
    fun appearanceFollowsTheSystemByDefaultAndIsRemembered() {
        assertEquals(Appearance.SYSTEM, SettingsStore(context).appearance.value)

        SettingsStore(context).setAppearance(Appearance.DARK)

        assertEquals(Appearance.DARK, SettingsStore(context).appearance.value)
    }

    @Test
    fun debugModeIsOffByDefault() {
        assertFalse(SettingsStore(context).debugMode.value)
    }

    @Test
    fun debugModeIsRememberedAcrossLaunches() {
        SettingsStore(context).enableDebugMode()

        assertTrue("un nouveau démarrage retrouve le mode debug", SettingsStore(context).debugMode.value)
    }

    @Test
    fun forcingTheOnboardingNeedsTheDebugMode() {
        val store = SettingsStore(context)

        store.setAlwaysShowOnboarding(true)
        assertFalse("sans mode debug, le réglage est ignoré", store.alwaysShowOnboarding.value)

        store.enableDebugMode()
        store.setAlwaysShowOnboarding(true)
        assertTrue(store.alwaysShowOnboarding.value)
        assertTrue("et il est conservé", SettingsStore(context).alwaysShowOnboarding.value)
    }
}
