package app.redflag.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The apps installed, removed or updated, as the launcher sees them ([LauncherApps]): no permission and no broadcast to declare,
 * and only while the flow is collected. Emits the package name of each change (several apps at once for an SD card that comes or
 * goes). Only apps the app can see are reported, which is what the lists show (launchable apps, see `<queries>` in the manifest).
 */
fun packageChanges(context: Context): Flow<String> = callbackFlow {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    if (launcherApps == null) {
        awaitClose()
        return@callbackFlow
    }
    val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = send(packageName)
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = send(packageName)
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = send(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            packageNames.orEmpty().forEach(::send)
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            packageNames.orEmpty().forEach(::send)

        private fun send(packageName: String?) {
            if (packageName != null) trySend(packageName)
        }
    }
    launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
    awaitClose { launcherApps.unregisterCallback(callback) }
}
