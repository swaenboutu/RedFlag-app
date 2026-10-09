package app.redflag.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import app.redflag.R
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** One bar: its label and its three segments, bottom to top (closed, opened, postponed). */
data class ChartBar(val label: String, val blocked: Int, val bypassed: Int, val snoozed: Int) {
    val total: Int get() = blocked + bypassed + snoozed
}

/**
 * Stacked bar chart, drawn by hand (no library): the height of a bar is the number of attempts, and its three kinds of segment
 * say how each one ended. They are told apart by their look, not only by color: closed = solid navy, opened = hatched,
 * postponed = light slate with an outline.
 */
class StackedBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val ink = ContextCompat.getColor(context, R.color.chart_ink)

    private val closedPaint = fill(R.color.chart_blocked)
    private val openedFill = fill(R.color.chart_bypassed)
    private val snoozedFill = fill(R.color.chart_snoozed)
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = ink
    }
    private val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        color = ink
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_grid)
        strokeWidth = 1 * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@StackedBarChartView, com.google.android.material.R.attr.colorOnSurfaceVariant)
        textSize = 13 * scaledDensity
    }
    private val currentPaint = Paint(textPaint).apply {
        color = ink
        typeface = Typeface.DEFAULT_BOLD
    }

    private var bars: List<ChartBar> = emptyList()
    private val rect = RectF()
    private val clip = Path()

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = ContextCompat.getColor(context, color) }

    fun setBars(bars: List<ChartBar>) {
        this.bars = bars
        invalidate()
    }

    /** Top of the scale: every whole number up to 4 (3 at least), then a multiple of 4 so the four steps stay whole. */
    private fun scaleTop(): Int {
        val most = bars.maxOfOrNull { it.total } ?: 0
        return if (most <= 4) max(3, most) else ceil(most / 4.0).toInt() * 4
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val top = scaleTop()
        val steps = min(top, 4)
        val fontHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
        val axisWidth = textPaint.measureText(top.toString()) + 12 * density
        val bottomLabelsHeight = fontHeight + 8 * density
        val chartLeft = axisWidth
        val chartRight = width - 4 * density
        val chartTop = fontHeight / 2
        val chartBottom = height - bottomLabelsHeight

        // Horizontal guides, from 0 to the top of the scale.
        for (step in 0..steps) {
            val y = chartBottom - (chartBottom - chartTop) * step / steps.toFloat()
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            val value = (top * step / steps).toString()
            canvas.drawText(value, axisWidth - 8 * density - textPaint.measureText(value), y + fontHeight / 4, textPaint)
        }

        val slot = (chartRight - chartLeft) / bars.size
        val barWidth = min(slot * 0.62f, 30 * density)
        val labelWidth = bars.maxOf { currentPaint.measureText(it.label) } + 6 * density
        // Labels too tight: only write one out of two, three...
        val labelEvery = max(1, ceil(labelWidth / slot).toInt())
        val gap = 2 * density

        bars.forEachIndexed { index, bar ->
            val left = chartLeft + slot * index + (slot - barWidth) / 2
            var bottom = chartBottom
            for ((count, kind) in listOf(bar.blocked to Kind.CLOSED, bar.bypassed to Kind.OPENED, bar.snoozed to Kind.SNOOZED)) {
                if (count == 0) continue
                val segmentTop = bottom - (chartBottom - chartTop) * count / top
                rect.set(left, segmentTop + gap / 2, left + barWidth, bottom - gap / 2)
                drawSegment(canvas, kind)
                bottom = segmentTop
            }
            if ((bars.size - 1 - index) % labelEvery == 0) {
                val paint = if (index == bars.lastIndex) currentPaint else textPaint
                val x = left + barWidth / 2 - paint.measureText(bar.label) / 2
                canvas.drawText(bar.label, x, height - 6 * density, paint)
            }
        }
    }

    private enum class Kind { CLOSED, OPENED, SNOOZED }

    private fun drawSegment(canvas: Canvas, kind: Kind) {
        val radius = min(6 * density, min(rect.width(), rect.height()) / 2)
        when (kind) {
            Kind.CLOSED -> canvas.drawRoundRect(rect, radius, radius, closedPaint)
            Kind.OPENED -> {
                canvas.drawRoundRect(rect, radius, radius, openedFill)
                canvas.save()
                clip.reset()
                clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
                canvas.clipPath(clip)
                val step = 6 * density
                var x = rect.left - rect.height()
                while (x < rect.right) {
                    canvas.drawLine(x, rect.bottom, x + rect.height(), rect.top, hatch)
                    x += step
                }
                canvas.restore()
                strokeSegment(canvas, radius)
            }
            Kind.SNOOZED -> {
                canvas.drawRoundRect(rect, radius, radius, snoozedFill)
                strokeSegment(canvas, radius)
            }
        }
    }

    /** The outline stays inside the segment, so neighbours keep their gap. */
    private fun strokeSegment(canvas: Canvas, radius: Float) {
        val half = outline.strokeWidth / 2
        rect.inset(half, half)
        canvas.drawRoundRect(rect, radius - half, radius - half, outline)
        rect.inset(-half, -half)
    }
}
