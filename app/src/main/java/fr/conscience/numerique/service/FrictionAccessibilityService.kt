package fr.conscience.numerique.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import fr.conscience.numerique.ConscienceApp
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

    private val container get() = (application as ConscienceApp).container

    override fun onServiceConnected() {
        scope.launch {
            container.repository.monitoredApps.collect { list ->
                monitored = list.associateBy { it.app.packageName }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == SYSTEM_UI || isInputMethod(pkg)) return

        val gate = container.frictionGate
        val target = monitored[pkg]
        if (target == null) {
            gate.onOtherAppForeground(pkg)
            return
        }
        if (gate.isAllowed(pkg) || target.app.snoozedUntil > System.currentTimeMillis()) return
        startActivity(InterstitialActivity.intent(this, pkg))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun isInputMethod(pkg: String): Boolean {
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
    }
}
