package fr.conscience.numerique.ui.common

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationBarView
import fr.conscience.numerique.R
import fr.conscience.numerique.ui.apps.MainActivity
import fr.conscience.numerique.ui.problems.ProblemsManagerActivity
import fr.conscience.numerique.ui.settings.SettingsActivity
import fr.conscience.numerique.ui.stats.StatsActivity

/**
 * Barre de navigation commune : Applications, Problématiques, Statistiques, Réglages.
 *
 * Les onglets ne s'empilent pas : la pile est toujours « Applications » (la racine), éventuellement suivie de l'onglet
 * affiché. Changer d'onglet, ou toucher celui d'un écran de détail, ne crée donc jamais de pile de six écrans.
 */
object BottomNav {
    /**
     * [current] : l'onglet de l'écran qui appelle (R.id.nav_apps, R.id.nav_problems…).
     * [isTabRoot] : faux pour un écran de détail ouvert depuis cet onglet (fiche d'une app, statistiques d'une app) :
     * toucher l'onglet ramène alors à la liste de l'onglet, au lieu de ne rien faire.
     */
    fun setup(activity: Activity, nav: NavigationBarView, current: Int, isTabRoot: Boolean = true) {
        select(nav, current)
        extendIntoGestureArea(activity, nav)
        nav.setOnItemSelectedListener { item ->
            when {
                item.itemId == current && isTabRoot -> true
                // Écran de détail : on referme simplement le détail pour retrouver la liste de l'onglet, déjà ouverte dessous.
                item.itemId == current -> {
                    activity.finish()
                    false
                }
                else -> {
                    open(activity, item.itemId)
                    false
                }
            }
        }
    }

    /**
     * Makes the menu reach the bottom edge of the screen. The screens pad their whole root with the system bars (fitsSystemWindows),
     * which left a strip of page color under the menu, where the gesture handle is. Here the root keeps the top and side insets only,
     * and the bottom inset becomes padding of the menu's own container, which has the menu's color.
     */
    private fun extendIntoGestureArea(activity: Activity, nav: View) {
        val bar = nav.parent as? View ?: return
        val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        root.fitsSystemWindows = false
        bar.setBackgroundColor(MaterialColors.getColor(bar, com.google.android.material.R.attr.colorSurface))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            bar.setPadding(0, 0, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun open(activity: Activity, tab: Int) {
        val root = Intent(activity, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        val target = when (tab) {
            R.id.nav_problems -> ProblemsManagerActivity::class.java
            R.id.nav_stats -> StatsActivity::class.java
            R.id.nav_settings -> SettingsActivity::class.java
            else -> null
        }
        if (target == null) {
            // « Applications » : retour à l'écran racine, les écrans au-dessus sont fermés.
            activity.startActivity(root)
        } else {
            // La racine d'abord (elle ferme tout ce qui était au-dessus), puis l'onglet demandé : la pile est [racine, onglet].
            activity.startActivities(arrayOf(root, Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
        }
    }

    /**
     * À rappeler au retour sur l'écran : l'onglet sélectionné doit rester le sien. On coche l'entrée du menu plutôt que d'utiliser
     * `selectedItemId`, qui déclencherait l'écouteur de navigation (et, sur un écran de détail, refermerait l'écran).
     */
    fun select(nav: NavigationBarView, current: Int) {
        nav.menu.findItem(current)?.isChecked = true
    }
}
