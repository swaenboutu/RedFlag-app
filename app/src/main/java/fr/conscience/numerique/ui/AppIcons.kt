package fr.conscience.numerique.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** L'icône de l'app [packageName], donnée par le système ; icône par défaut si l'app a été désinstallée depuis. */
fun Context.iconOf(packageName: String): Drawable = try {
    packageManager.getApplicationIcon(packageName)
} catch (_: PackageManager.NameNotFoundException) {
    packageManager.defaultActivityIcon
}
