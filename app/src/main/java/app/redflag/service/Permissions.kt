package app.redflag.service

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/** The two Android permissions the interruption needs, in the order they are asked for. */
enum class FrictionPermission {
    /** "Usage access": tells which app comes to the foreground. */
    USAGE_ACCESS,

    /** "Display over other apps": lets the interruption screen open on top of that app, from the background. */
    OVERLAY,
}

/** True if the user allowed "Usage access" for our app in Android's settings. */
fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    // MODE_DEFAULT: the permission itself decides (some devices store the choice there).
    if (mode == AppOpsManager.MODE_DEFAULT) {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/** True if the user allowed "Display over other apps" for our app. */
fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

/** The first permission still missing, or null when the interruption can work. */
fun missingPermission(context: Context): FrictionPermission? = when {
    !hasUsageAccess(context) -> FrictionPermission.USAGE_ACCESS
    !hasOverlayPermission(context) -> FrictionPermission.OVERLAY
    else -> null
}

/** True once both permissions are granted. */
fun hasFrictionPermissions(context: Context): Boolean = missingPermission(context) == null

/** The Android settings action where [permission] is granted. */
fun settingsAction(permission: FrictionPermission): String = when (permission) {
    FrictionPermission.USAGE_ACCESS -> Settings.ACTION_USAGE_ACCESS_SETTINGS
    FrictionPermission.OVERLAY -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION
}

/**
 * Opens the Android settings screen of [permission], straight on our app when the device supports it (the package URI), otherwise
 * on the list of apps (some devices have no screen for the package URI).
 */
fun openPermissionSettings(context: Context, permission: FrictionPermission) {
    val action = settingsAction(permission)
    try {
        context.startActivity(Intent(action, Uri.parse("package:${context.packageName}")))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(action))
    }
}
