package app.redflag.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App settings, stored on the device only (SharedPreferences, excluded from backups).
 * [now] can be replaced to test time.
 */
class SettingsStore(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _interceptionEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    private val _pauseMinutes = MutableStateFlow(prefs.getInt(KEY_PAUSE_MINUTES, DEFAULT_PAUSE_MINUTES))
    private val _hideSystemApps = MutableStateFlow(prefs.getBoolean(KEY_HIDE_SYSTEM_APPS, DEFAULT_HIDE_SYSTEM_APPS))

    private val _appearance = MutableStateFlow(Appearance.fromKey(prefs.getString(KEY_APPEARANCE, null)))

    /** Light, dark, or follow the phone (default). Applied app-wide with [Appearance.apply]. */
    val appearance: StateFlow<Appearance> = _appearance

    fun setAppearance(value: Appearance) {
        prefs.edit { putString(KEY_APPEARANCE, value.key) }
        _appearance.value = value
    }

    private val _reenableAt = MutableStateFlow(prefs.getLong(KEY_REENABLE_AT, 0L))

    /**
     * Main switch: false = no interruption at all, whatever the flagged apps. When the switch was turned off for a while,
     * [reenableAt] says when it comes back on by itself; [isInterceptionActive] is the one to ask whether to interrupt.
     */
    val interceptionEnabled: StateFlow<Boolean> = _interceptionEnabled

    /** When a timed deactivation ends (epoch milliseconds); 0 = none (switch on, or off until the user turns it back on). */
    val reenableAt: StateFlow<Long> = _reenableAt

    /**
     * Whether flagged apps must be interrupted now: the switch is on, or a timed deactivation is over (the switch is then turned
     * back on for good, so the screens catch up).
     */
    fun isInterceptionActive(): Boolean {
        if (_interceptionEnabled.value) return true
        val at = _reenableAt.value
        if (at == 0L || now() < at) return false
        setInterceptionEnabled(true)
        return true
    }

    /** Durée (en minutes) pendant laquelle « Ne plus me demander » suspend l'interruption d'une app. */
    val pauseMinutes: StateFlow<Int> = _pauseMinutes

    /** Vrai = la liste des applications masque les apps système (celles mises à jour par l'utilisateur restent). */
    val hideSystemApps: StateFlow<Boolean> = _hideSystemApps

    private val _debugMode = MutableStateFlow(prefs.getBoolean(KEY_DEBUG_MODE, false))

    /**
     * Mode debug : la section « Debug » des Réglages est visible. Il s'active en appuyant 7 fois sur le numéro de version
     * (voir [DebugUnlock]), est conservé ensuite, et disparaît avec « Réinitialiser l'application » (qui efface ces réglages).
     */
    val debugMode: StateFlow<Boolean> = _debugMode

    fun enableDebugMode() {
        prefs.edit { putBoolean(KEY_DEBUG_MODE, true) }
        _debugMode.value = true
    }

    /** Pour les tests : revient à l'état d'une première utilisation (la réinitialisation de l'app, elle, efface tous les réglages). */
    @androidx.annotation.VisibleForTesting
    fun disableDebugMode() {
        prefs.edit { putBoolean(KEY_DEBUG_MODE, false) }
        _debugMode.value = false
        _alwaysShowOnboarding.value = false
    }

    private val _alwaysShowOnboarding = MutableStateFlow(_debugMode.value && prefs.getBoolean(KEY_ALWAYS_SHOW_ONBOARDING, false))

    /**
     * Debug : vrai = l'accueil s'affiche à chaque lancement de l'app depuis son icône, sans effacer les données (voir
     * [shouldShowOnboarding]). Un lancement, et non un démarrage du processus : le service d'accessibilité garde le processus
     * en vie, qui ne repart alors jamais de zéro.
     */
    val alwaysShowOnboarding: StateFlow<Boolean> = _alwaysShowOnboarding

    /** Vrai une fois l'écran d'accueil passé : il ne s'affiche plus ensuite (sauf si le réglage de debug le force). */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    fun setAlwaysShowOnboarding(value: Boolean) {
        if (!_debugMode.value) return
        prefs.edit { putBoolean(KEY_ALWAYS_SHOW_ONBOARDING, value) }
        _alwaysShowOnboarding.value = value
    }

    /** Turns the main switch on, or off until the user turns it back on (any timed deactivation is forgotten). */
    fun setInterceptionEnabled(value: Boolean) {
        prefs.edit {
            putBoolean(KEY_ENABLED, value)
            putLong(KEY_REENABLE_AT, 0L)
        }
        _reenableAt.value = 0L
        _interceptionEnabled.value = value
    }

    /** Turns the main switch off for [minutes] minutes: it comes back on by itself afterwards. */
    fun disableInterceptionFor(minutes: Int) {
        val at = now() + minutes * 60_000L
        prefs.edit {
            putBoolean(KEY_ENABLED, false)
            putLong(KEY_REENABLE_AT, at)
        }
        _reenableAt.value = at
        _interceptionEnabled.value = false
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
        /**
         * L'accueil doit-il s'afficher ? Au premier lancement ; ou, si le réglage de debug le demande, à chaque lancement de
         * l'app depuis son icône ([freshLaunch]), pas quand on revient sur un écran déjà ouvert ni quand l'app se rouvre d'elle-même.
         */
        fun shouldShowOnboarding(done: Boolean, alwaysShow: Boolean, freshLaunch: Boolean): Boolean =
            !done || (alwaysShow && freshLaunch)

        const val DEFAULT_PAUSE_MINUTES = 60

        /**
         * Par défaut, les apps système (horloge, calculatrice…) ne sont pas proposées : elles sont rarement à signaler et
         * une longue liste fait peur. Valable partout (accueil, liste des apps, sélecteur), l'utilisateur peut les réafficher.
         */
        const val DEFAULT_HIDE_SYSTEM_APPS = true

        /** Durées proposées, en minutes. */
        val PAUSE_CHOICES = listOf(15, 60, 180, 24 * 60)

        /** Durations offered when turning the main switch off, in minutes (the dialog adds "until I turn it back on"). */
        val OFF_CHOICES = listOf(60, 12 * 60, 24 * 60)

        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_ENABLED = "interception_enabled"
        private const val KEY_REENABLE_AT = "interception_reenable_at"
        private const val KEY_PAUSE_MINUTES = "pause_minutes"
        private const val KEY_HIDE_SYSTEM_APPS = "hide_system_apps"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ALWAYS_SHOW_ONBOARDING = "always_show_onboarding"
        private const val KEY_DEBUG_MODE = "debug_mode"
    }
}
