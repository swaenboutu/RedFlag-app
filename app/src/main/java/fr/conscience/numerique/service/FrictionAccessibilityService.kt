package fr.conscience.numerique.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import fr.conscience.numerique.container
import fr.conscience.numerique.data.MonitoredAppWithProblems
import fr.conscience.numerique.ui.InterstitialActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Détecte uniquement le nom du paquet qui passe au premier plan (canRetrieveWindowContent=false :
 * aucun contenu d'écran n'est lu) et affiche l'interstitiel pour les apps labelisées.
 */
class FrictionAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var monitored: Map<String, MonitoredAppWithProblems> = emptyMap()

    /** Écran éteint : tous les « Oui » sont oubliés, l'app redemandera à la reprise. */
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = container.frictionGate.revokeAll()
    }

    private var homePackages: Set<String> = emptySet()
    private var homePackagesAt = 0L

    override fun onServiceConnected() {
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        scope.launch {
            container.repository.monitoredApps.collect { list ->
                monitored = list.associateBy { it.app.packageName }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!container.settings.interceptionEnabled.value) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == SYSTEM_UI || isInputMethod(pkg)) return

        val gate = container.frictionGate
        val target = monitored[pkg]

        // Une feuille de partage, une fenêtre d'autorisation… sont des fenêtres passagères du système : elles ne comptent
        // pas comme « une autre app » et ne doivent pas faire oublier un « Oui ». Seules les vraies apps et l'accueil comptent.
        if (target != null || isHome(pkg) || hasLauncherEntry(pkg)) gate.onOtherAppForeground(pkg)
        if (target == null) return

        if (gate.isDeclineGuarded(pkg)) return
        if (gate.isAllowed(pkg) || target.app.snoozedUntil > System.currentTimeMillis()) return
        startActivity(InterstitialActivity.intent(this, pkg))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOff) }
        scope.cancel()
        super.onDestroy()
    }

    private fun isInputMethod(pkg: String): Boolean {
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    /** Vrai pour une app qu'on peut lancer depuis l'écran d'accueil (par opposition aux services et fenêtres du système). */
    private fun hasLauncherEntry(pkg: String): Boolean = packageManager.getLaunchIntentForPackage(pkg) != null

    /** Vrai si [pkg] est l'écran d'accueil (lanceur) du téléphone. La liste est relue au plus une fois par minute. */
    private fun isHome(pkg: String): Boolean {
        val time = System.currentTimeMillis()
        if (time - homePackagesAt > HOME_CACHE_MILLIS) {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            homePackages = packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName }.toSet()
            homePackagesAt = time
        }
        return pkg in homePackages
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
        const val HOME_CACHE_MILLIS = 60_000L
    }
}
