package app.redflag.ui.common

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import app.redflag.R
import com.google.android.material.color.MaterialColors

const val CARD_RADIUS_DP = 20
const val CARD_GAP_DP = 10

/**
 * The card background of the lists, outlined like the cards of the Home screen. Several consecutive rows form one card: the first
 * one rounds the top, the last one rounds the bottom and leaves [gapAfterDp] before the next card.
 */
fun View.applyCard(roundTop: Boolean, roundBottom: Boolean, gapAfterDp: Int) {
    val density = resources.displayMetrics.density
    val radius = CARD_RADIUS_DP * density
    val top = if (roundTop) radius else 0f
    val bottom = if (roundBottom) radius else 0f
    val surface = MaterialColors.getColor(this@applyCard, com.google.android.material.R.attr.colorSurface)
    background = outlinedCardBackground(surface, top, bottom, density)
    (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        it.bottomMargin = (gapAfterDp * density).toInt()
        layoutParams = it
    }
}

/**
 * A row of an outlined card: a line-colored shape with the surface drawn 1 dp inside it, so stacked rows show their outline on the
 * sides only and the first and last ones also on the top and bottom.
 */
private fun View.outlinedCardBackground(surface: Int, top: Float, bottom: Float, density: Float): Drawable {
    val line = ContextCompat.getColor(context, R.color.line)
    val stroke = maxOf(1, density.toInt())
    fun shape(color: Int, t: Float, b: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadii = floatArrayOf(t, t, t, t, b, b, b, b)
    }
    val inner = shape(surface, maxOf(0f, top - stroke), maxOf(0f, bottom - stroke))
    return LayerDrawable(arrayOf(shape(line, top, bottom), inner)).apply {
        setLayerInset(1, stroke, if (top > 0f) stroke else 0, stroke, if (bottom > 0f) stroke else 0)
    }
}

fun Int.dp(view: View): Int = (this * view.resources.displayMetrics.density).toInt()
