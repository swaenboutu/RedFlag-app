package fr.conscience.numerique.ui

import android.app.Activity
import android.content.Intent
import com.google.android.material.navigation.NavigationBarView
import fr.conscience.numerique.R

/** Barre de navigation commune : Applications, Problématiques, Statistiques, Réglages. */
object BottomNav {
    /** [current] : identifiant de l'onglet de l'écran qui appelle (R.id.nav_apps, R.id.nav_problems…). */
    fun setup(activity: Activity, nav: NavigationBarView, current: Int) {
        nav.selectedItemId = current
        nav.setOnItemSelectedListener { item ->
            when {
                item.itemId == current -> true
                item.itemId == R.id.nav_apps -> {
                    // Retour à l'écran racine (les écrans au-dessus sont fermés).
                    activity.startActivity(
                        Intent(activity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    )
                    false
                }
                item.itemId == R.id.nav_problems -> {
                    activity.startActivity(
                        Intent(activity, ProblemsManagerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    )
                    false
                }
                item.itemId == R.id.nav_stats -> {
                    activity.startActivity(
                        Intent(activity, StatsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    )
                    false
                }
                else -> {
                    activity.startActivity(
                        Intent(activity, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    )
                    false
                }
            }
        }
    }

    /** À rappeler au retour sur l'écran : l'onglet sélectionné doit rester le sien. */
    fun select(nav: NavigationBarView, current: Int) {
        nav.selectedItemId = current
    }
}
