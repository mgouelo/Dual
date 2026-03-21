package fr.iutvannes.dual.model.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Vue graphique pour afficher un graphe de points
 */
class GraphView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var data: List<Pair<String, Float>> = emptyList()
        set(value) { field = value; invalidate() }

    var yMin: Int = 0
        set(value) { field = value; invalidate() }

    var yMax: Int = 10
        set(value) { field = value; invalidate() }

    var yLabels: List<Float>? = null
        set(value) { field = value; invalidate() }

    var lineColor: Int = Color.BLUE
        set(value) { field = value; invalidate() }

    var labelColor: Int = Color.BLACK
    var gridColor: Int  = Color.LTGRAY

    private val linePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f; style = Paint.Style.STROKE }
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f; style = Paint.Style.STROKE }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        linePaint.color  = lineColor
        pointPaint.color = lineColor
        textPaint.color  = labelColor
        gridPaint.color  = gridColor

        val textSize = (width / 30f).coerceIn(18f, 32f)
        textPaint.textSize = textSize

        val labels = data.map { pair -> pair.first }

        val marginLeft   = textSize * 3.5f
        val marginTop    = textSize
        val maxLabelLen  = labels.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val marginBottom = maxLabelLen + textSize * 1.5f

        // Padding horizontal : les points ne touchent pas les bords
        val paddingX = textSize * 2f
        val drawWidth  = width  - marginLeft - paddingX * 2
        val drawHeight = height - marginBottom - marginTop
        val heightRange = if (yMax != yMin) (yMax - yMin).toFloat() else 1f

        // ── Grille + labels Y ───────────────────────────────────────────────
        val labelsY: List<Float> = yLabels ?: (0..(yMax - yMin)).map { yMin + it.toFloat() }

        labelsY.forEachIndexed { i, value ->
            val ratio = if (yLabels != null)
                i.toFloat() / (labelsY.size - 1).toFloat().coerceAtLeast(1f)
            else
                (value - yMin) / heightRange

            val y = marginTop + drawHeight - ratio * drawHeight
            canvas.drawLine(marginLeft, y, width.toFloat() - paddingX, y, gridPaint)

            val label = if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, marginLeft - 8f, y + textSize / 3f, textPaint)
        }

        // ── Points et ligne ─────────────────────────────────────────────────
        fun xOf(i: Int) = if (data.size == 1) marginLeft + paddingX + drawWidth / 2f
        else marginLeft + paddingX + i * (drawWidth / (data.size - 1).toFloat())

        fun yOf(v: Float): Float {
            return if (yLabels != null) {
                val labMin = yLabels!!.first()
                val labMax = yLabels!!.last()
                val range = if (labMax != labMin) labMax - labMin else 1f
                marginTop + drawHeight - ((v - labMin) / range) * drawHeight
            } else {
                marginTop + drawHeight - ((v - yMin) / heightRange) * drawHeight
            }
        }

        data.forEachIndexed { i, pair ->
            canvas.drawCircle(xOf(i), yOf(pair.second), textSize * 0.4f, pointPaint)
        }
        for (i in 0 until data.size - 1) {
            canvas.drawLine(xOf(i), yOf(data[i].second), xOf(i + 1), yOf(data[i + 1].second), linePaint)
        }

        // ── Labels X : -90° centré sous chaque point ────────────────────────
        textPaint.textAlign = Paint.Align.LEFT

        labels.forEachIndexed { i, label ->
            val x = xOf(i)
            canvas.save()
            canvas.rotate(-90f, x, height.toFloat())
            canvas.drawText(label, x, height.toFloat(), textPaint)
            canvas.restore()
        }
    }
}