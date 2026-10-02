package fr.conscience.numerique.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable)

/** Liste les apps lançables via PackageManager ; les icônes viennent du système, jamais embarquées. */
class InstalledAppsProvider(private val context: Context) {
    /**
     * [hideSystemApps] : masque les apps système, sauf celles que l'utilisateur a mises à jour
     * (Chrome, par exemple, est une app système mise à jour depuis le Play Store).
     */
    @Suppress("DEPRECATION")
    fun list(hideSystemApps: Boolean = SettingsStore.DEFAULT_HIDE_SYSTEM_APPS): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .filter { !hideSystemApps || !it.activityInfo.applicationInfo.isPurelySystem() }
            .distinctBy { it.activityInfo.packageName }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
    }

    private fun ApplicationInfo.isPurelySystem() =
        flags and ApplicationInfo.FLAG_SYSTEM != 0 && flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
}
