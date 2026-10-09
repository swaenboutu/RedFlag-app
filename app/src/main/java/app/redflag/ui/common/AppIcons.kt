package app.redflag.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import app.redflag.container

/** L'icône de l'app [packageName], donnée par le système et gardée en mémoire ; icône par défaut si l'app a été désinstallée depuis. */
fun Context.iconOf(packageName: String): Drawable = container.icons.get(packageName)
