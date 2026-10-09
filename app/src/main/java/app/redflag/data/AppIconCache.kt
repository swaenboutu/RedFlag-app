package app.redflag.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Les icônes des apps, gardées en mémoire : les écrans les demandent pendant l'affichage d'une ligne, sur le fil principal, où une
 * lecture par le système (avec décodage) fait saccader le défilement. Le cache est rempli au démarrage pour les apps signalées
 * ([preload], hors du fil principal) et par la liste des apps installées ; une icône absente est lue à la demande, puis gardée.
 * Un cache ne garde que l'état partagé de l'icône : chaque appel rend un nouveau [Drawable], qu'un écran peut modifier sans gêner les autres.
 */
class AppIconCache(private val context: Context) {
    private val cache = LruCache<String, Drawable.ConstantState>(MAX_ICONS)

    /** L'icône de l'app [packageName] ; icône par défaut si l'app a été désinstallée depuis. */
    fun get(packageName: String): Drawable {
        cache[packageName]?.let { return it.newDrawable(context.resources) }
        val icon = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return context.packageManager.defaultActivityIcon
        }
        put(packageName, icon)
        return icon
    }

    fun put(packageName: String, icon: Drawable) {
        icon.constantState?.let { cache.put(packageName, it) }
    }

    /** Charge à l'avance les icônes de [packages], sur un fil d'arrière-plan. */
    suspend fun preload(packages: Collection<String>) = withContext(Dispatchers.IO) {
        packages.forEach { get(it) }
    }

    private companion object {
        const val MAX_ICONS = 256
    }
}
