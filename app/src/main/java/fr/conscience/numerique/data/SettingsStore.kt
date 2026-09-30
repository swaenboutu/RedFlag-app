package fr.conscience.numerique.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Réglages de l'app, stockés uniquement sur l'appareil (SharedPreferences, exclues des sauvegardes). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _interceptionEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    private val _pauseMinutes = MutableStateFlow(prefs.getInt(KEY_PAUSE_MINUTES, DEFAULT_PAUSE_MINUTES))
    private val _hideSystemApps = MutableStateFlow(prefs.getBoolean(KEY_HIDE_SYSTEM_APPS, false))

    /** Interrupteur général : faux = plus aucune interruption, quelles que soient les apps signalées. */
    val interceptionEnabled: StateFlow<Boolean> = _interceptionEnabled

    /** Durée (en minutes) pendant laquelle « Ne plus me demander » suspend l'interruption d'une app. */
    val pauseMinutes: StateFlow<Int> = _pauseMinutes

    /** Vrai = la liste des applications masque les apps système (celles mises à jour par l'utilisateur restent). */
    val hideSystemApps: StateFlow<Boolean> = _hideSystemApps

    /** Version de développement (débogable) : les réglages de debug n'existent pas dans une version de production. */
    val isDebuggable: Boolean = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val _alwaysShowOnboarding = MutableStateFlow(isDebuggable && prefs.getBoolean(KEY_ALWAYS_SHOW_ONBOARDING, false))

    /** Debug : vrai = l'accueil s'affiche à chaque démarrage de l'app (processus neuf), sans effacer les données. */
    val alwaysShowOnboarding: StateFlow<Boolean> = _alwaysShowOnboarding

    /** Vrai une fois l'écran d'accueil passé : il ne s'affiche qu'au premier lancement (ou à chaque démarrage si forcé). */
    var onboardingDone: Boolean
        get() = if (_alwaysShowOnboarding.value) onboardingSeenThisRun else prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) {
            onboardingSeenThisRun = value
            prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }
        }

    fun setAlwaysShowOnboarding(value: Boolean) {
        if (!isDebuggable) return
        prefs.edit { putBoolean(KEY_ALWAYS_SHOW_ONBOARDING, value) }
        // La session en cours a dépassé l'accueil : il ne réapparaît qu'au prochain démarrage, pas en plein réglage.
        if (value) onboardingSeenThisRun = true
        _alwaysShowOnboarding.value = value
    }

    fun setInterceptionEnabled(value: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        _interceptionEnabled.value = value
    }

    fun setPauseMinutes(value: Int) {
        prefs.edit { putInt(KEY_PAUSE_MINUTES, value) }
        _pauseMinutes.value = value
    }

    fun setHideSystemApps(value: Boolean) {
        prefs.edit { putBoolean(KEY_HIDE_SYSTEM_APPS, value) }
        _hideSystemApps.value = value
    }

    companion object {
        /** Accueil déjà passé depuis le démarrage du processus : évite de le relancer en boucle quand il est forcé (réglage de debug). */
        @Volatile
        private var onboardingSeenThisRun = false

        const val DEFAULT_PAUSE_MINUTES = 60

        /** Durées proposées, en minutes. */
        val PAUSE_CHOICES = listOf(15, 60, 180, 24 * 60)

        private const val KEY_ENABLED = "interception_enabled"
        private const val KEY_PAUSE_MINUTES = "pause_minutes"
        private const val KEY_HIDE_SYSTEM_APPS = "hide_system_apps"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ALWAYS_SHOW_ONBOARDING = "always_show_onboarding"
    }
}
