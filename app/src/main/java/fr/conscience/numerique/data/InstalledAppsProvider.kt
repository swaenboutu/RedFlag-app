package fr.conscience.numerique.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable)

/** Liste les apps lançables via PackageManager ; les icônes viennent du système, jamais embarquées. */
class InstalledAppsProvider(private val context: Context, private val icons: AppIconCache? = null) {
    /**
     * [hideSystemApps] : masque les apps système, sauf celles que l'utilisateur a mises à jour
     * (Chrome, par exemple, est une app système mise à jour depuis le Play Store).
     *
     * [alwaysInclude] : paquets à lister quoi qu'il arrive (les apps signalées : une app sous surveillance doit
     * rester visible, pour pouvoir être retirée, même si le réglage masque les apps système).
     */
    @Suppress("DEPRECATION")
    fun list(
        hideSystemApps: Boolean = SettingsStore.DEFAULT_HIDE_SYSTEM_APPS,
        alwaysInclude: Set<String> = emptySet(),
    ): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .filter {
                isListed(it.activityInfo.packageName, it.activityInfo.applicationInfo.isPurelySystem(), hideSystemApps, alwaysInclude)
            }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val icon = it.loadIcon(pm)
                icons?.put(it.activityInfo.packageName, icon)
                InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), icon)
            }
    }

    private fun ApplicationInfo.isPurelySystem() =
        flags and ApplicationInfo.FLAG_SYSTEM != 0 && flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0

    companion object {
        /** Une app est listée si elle n'est pas une app système masquée, ou si elle fait partie de [alwaysInclude]. */
        fun isListed(packageName: String, purelySystem: Boolean, hideSystemApps: Boolean, alwaysInclude: Set<String>): Boolean =
            !hideSystemApps || !purelySystem || packageName in alwaysInclude
    }
}
