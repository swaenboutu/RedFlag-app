package fr.conscience.numerique.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import fr.conscience.numerique.R
import kotlin.math.ceil
import kotlin.math.max

/** Une barre : son étiquette et ses trois segments, du bas vers le haut (fermée, outrepassée, mise en pause). */
data class ChartBar(val label: String, val blocked: Int, val bypassed: Int, val snoozed: Int) {
    val total: Int get() = blocked + bypassed + snoozed
}

/**
 * Graphique en barres empilées, dessiné à la main (aucune bibliothèque) : la hauteur d'une barre est le nombre total
 * de tentatives, ses couleurs disent comment chacune s'est terminée.
 */
class StackedBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val blockedPaint = fill(R.color.chart_blocked)
    private val bypassedPaint = fill(R.color.chart_bypassed)
    private val snoozedPaint = fill(R.color.chart_snoozed)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_grid)
        strokeWidth = 1 * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@StackedBarChartView, com.google.android.material.R.attr.colorOnSurfaceVariant)
        textSize = 11 * scaledDensity
    }

    private var bars: List<ChartBar> = emptyList()
    private val barRect = RectF()

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = ContextCompat.getColor(context, color) }

    fun setBars(bars: List<ChartBar>) {
        this.bars = bars
        invalidate()
    }

    /** Sommet de l'échelle : un multiple de 4 (au moins 4), pour des repères entiers à 25 %, 50 %, 75 % et 100 %. */
    private fun scaleTop(): Int = max(4, ceil(max(1, bars.maxOfOrNull { it.total } ?: 0) / 4.0).toInt() * 4)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val top = scaleTop()
        val fontHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
        val axisWidth = textPaint.measureText(top.toString()) + 8 * density
        val bottomLabelsHeight = fontHeight + 6 * density
        val chartLeft = axisWidth
        val chartRight = width - 4 * density
        val chartTop = fontHeight / 2
        val chartBottom = height - bottomLabelsHeight

        // Repères horizontaux : 0, 25 %, 50 %, 75 %, 100 % de l'échelle.
        for (step in 0..4) {
            val y = chartBottom - (chartBottom - chartTop) * step / 4f
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            val value = (top * step / 4).toString()
            canvas.drawText(value, axisWidth - 6 * density - textPaint.measureText(value), y + fontHeight / 4, textPaint)
        }

        val slot = (chartRight - chartLeft) / bars.size
        val barWidth = slot * 0.62f
        val labelWidth = bars.maxOf { textPaint.measureText(it.label) } + 6 * density
        // Étiquettes trop serrées : on n'en écrit qu\'une sur deux, trois…
        val labelEvery = max(1, ceil(labelWidth / slot).toInt())

        bars.forEachIndexed { index, bar ->
            val left = chartLeft + slot * index + (slot - barWidth) / 2
            var bottom = chartBottom
            for ((count, paint) in listOf(bar.blocked to blockedPaint, bar.bypassed to bypassedPaint, bar.snoozed to snoozedPaint)) {
                if (count == 0) continue
                val segmentTop = bottom - (chartBottom - chartTop) * count / top
                barRect.set(left, segmentTop, left + barWidth, bottom)
                canvas.drawRect(barRect, paint)
                bottom = segmentTop
            }
            if ((bars.size - 1 - index) % labelEvery == 0) {
                val x = left + barWidth / 2 - textPaint.measureText(bar.label) / 2
                canvas.drawText(bar.label, x, height - 4 * density, textPaint)
            }
        }
    }
}
