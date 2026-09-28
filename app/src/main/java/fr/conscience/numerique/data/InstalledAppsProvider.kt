package fr.conscience.numerique.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable)

/** Liste les apps lançables via PackageManager ; les icônes viennent du système, jamais embarquées. */
class InstalledAppsProvider(private val context: Context) {
    @Suppress("DEPRECATION")
    fun list(): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
    }
}
