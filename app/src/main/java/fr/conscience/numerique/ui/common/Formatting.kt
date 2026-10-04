package fr.conscience.numerique.ui.common

import android.content.Context
import fr.conscience.numerique.R

/** « 15 minutes », « 1 heure », « 24 heures » : dans la langue courante, avec le bon pluriel. */
fun formatPause(context: Context, minutes: Int): String =
    if (minutes % 60 == 0) {
        val hours = minutes / 60
        context.resources.getQuantityString(R.plurals.duration_hours, hours, hours)
    } else {
        context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
    }
