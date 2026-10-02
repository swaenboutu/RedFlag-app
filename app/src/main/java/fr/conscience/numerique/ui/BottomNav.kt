package fr.conscience.numerique.ui

import android.app.Activity
import android.content.Intent
import com.google.android.material.navigation.NavigationBarView
import fr.conscience.numerique.R

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
        nav.selectedItemId = current
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

    /** À rappeler au retour sur l'écran : l'onglet sélectionné doit rester le sien. */
    fun select(nav: NavigationBarView, current: Int) {
        nav.selectedItemId = current
    }
}
