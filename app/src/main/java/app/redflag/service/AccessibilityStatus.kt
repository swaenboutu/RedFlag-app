package app.redflag.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Vrai si l'utilisateur a activé notre service d'accessibilité dans les réglages Android. */
fun isFrictionServiceEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    val mine = ComponentName(context, FrictionAccessibilityService::class.java)
    // Android stocke la forme courte ("pkg/.Classe") ou complète : comparer les composants, pas les chaînes.
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
}
