package fr.conscience.numerique.data

import android.content.Context
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

    fun setInterceptionEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _interceptionEnabled.value = value
    }

    fun setPauseMinutes(value: Int) {
        prefs.edit().putInt(KEY_PAUSE_MINUTES, value).apply()
        _pauseMinutes.value = value
    }

    fun setHideSystemApps(value: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_SYSTEM_APPS, value).apply()
        _hideSystemApps.value = value
    }

    companion object {
        const val DEFAULT_PAUSE_MINUTES = 60

        /** Durées proposées, en minutes. */
        val PAUSE_CHOICES = listOf(15, 60, 180, 24 * 60)

        private const val KEY_ENABLED = "interception_enabled"
        private const val KEY_PAUSE_MINUTES = "pause_minutes"
        private const val KEY_HIDE_SYSTEM_APPS = "hide_system_apps"
    }
}
