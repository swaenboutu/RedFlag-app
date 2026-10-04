package fr.conscience.numerique.ui.common

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import com.google.android.material.color.MaterialColors

const val CARD_RADIUS_DP = 18
const val CARD_GAP_DP = 10

/**
 * Fond de « carte » blanche arrondie. Plusieurs lignes consécutives forment une seule carte :
 * la première arrondit le haut, la dernière arrondit le bas et laisse [gapAfterDp] avant la suivante.
 */
fun View.applyCard(roundTop: Boolean, roundBottom: Boolean, gapAfterDp: Int) {
    val density = resources.displayMetrics.density
    val top = if (roundTop) CARD_RADIUS_DP * density else 0f
    val bottom = if (roundBottom) CARD_RADIUS_DP * density else 0f
    background = GradientDrawable().apply {
        setColor(MaterialColors.getColor(this@applyCard, com.google.android.material.R.attr.colorSurface))
        cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
    }
    (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        it.bottomMargin = (gapAfterDp * density).toInt()
        layoutParams = it
    }
}

fun Int.dp(view: View): Int = (this * view.resources.displayMetrics.density).toInt()
