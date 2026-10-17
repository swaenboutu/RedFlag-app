package app.redflag.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable)

/** Lists the launchable apps through PackageManager; the icons come from the system, never bundled. */
class InstalledAppsProvider(private val context: Context, private val icons: AppIconCache? = null) {
    /**
     * [hideSystemApps]: hides the system apps, except those the user updated (Chrome, for example, is a system app updated from
     * the Play Store).
     *
     * [alwaysInclude]: packages listed whatever happens (the flagged apps: an app under watch must stay visible, so that it can be
     * removed, even when the setting hides the system apps).
     */
    fun list(
        hideSystemApps: Boolean = SettingsStore.DEFAULT_HIDE_SYSTEM_APPS,
        alwaysInclude: Set<String> = emptySet(),
    ): List<InstalledApp> {
        val pm = context.packageManager
        return launchable(hideSystemApps, alwaysInclude).map {
            val icon = it.loadIcon(pm)
            icons?.put(it.activityInfo.packageName, icon)
            InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), icon)
        }
    }

    /** How many apps [list] would return, without loading their icons and names (the expensive part). */
    fun count(
        hideSystemApps: Boolean = SettingsStore.DEFAULT_HIDE_SYSTEM_APPS,
        alwaysInclude: Set<String> = emptySet(),
    ): Int = launchable(hideSystemApps, alwaysInclude).size

    @Suppress("DEPRECATION")
    private fun launchable(hideSystemApps: Boolean, alwaysInclude: Set<String>): List<ResolveInfo> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .filter {
                isListed(it.activityInfo.packageName, it.activityInfo.applicationInfo.isPurelySystem(), hideSystemApps, alwaysInclude)
            }
            .distinctBy { it.activityInfo.packageName }
    }

    private fun ApplicationInfo.isPurelySystem() =
        flags and ApplicationInfo.FLAG_SYSTEM != 0 && flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0

    companion object {
        /** An app is listed unless it is a hidden system app, or when it is part of [alwaysInclude]. */
        fun isListed(packageName: String, purelySystem: Boolean, hideSystemApps: Boolean, alwaysInclude: Set<String>): Boolean =
            !hideSystemApps || !purelySystem || packageName in alwaysInclude
    }
}
