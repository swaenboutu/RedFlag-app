package fr.conscience.numerique.data

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
}
