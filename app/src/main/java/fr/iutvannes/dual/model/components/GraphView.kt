package fr.iutvannes.dual.model.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GraphView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Données : paire (label X, valeur Y), ex : (seance, score)
    var data: List<Pair<String, Float>> = emptyList()

    // Min et max Y
    var yMin: Int = 0
    var yMax: Int = 10

    // Couleurs
    var lineColor: Int = Color.BLUE
    var labelColor: Int = Color.BLACK
    var gridColor: Int = Color.LTGRAY

    // Paints
    // Apparence des lignes
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    // Apparence des textes
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
    }

    //Apparence de la grille
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    //Apparence des points
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        // Mettre à jour les couleurs
        linePaint.color = lineColor
        textPaint.color = labelColor
        gridPaint.color = gridColor

        // Distance entre de point du graphe avec une marge
        val widthStep = (width.toFloat() - 200) / (data.size - 1)
        val heightRange = yMax - yMin
        val heightStep = height - 100

        // Tracer la grille horizontale
        for (i in 0..heightRange) {
            val y = heightStep - (i / heightRange.toFloat()) * heightStep + 50f
            canvas.drawLine(15f, y, width.toFloat(), y, gridPaint)
        }

        // Placer les points
        for ((i, pair) in data.withIndex()) {
            val x = i * widthStep + 100
            val y = heightStep - ((pair.second - yMin) / heightRange) * heightStep + 50f
            canvas.drawCircle(x, y, 8f, pointPaint) // rayon = 8px
        }

        // Tracer la ligne
        for (i in 0 until data.size - 1) {
            val x1 = i * widthStep + 100
            // .second permet de récupérer la valeur de la paire
            val y1 = heightStep - ((data[i].second - yMin) / heightRange) * heightStep + 50f
            val x2 = (i + 1) * widthStep + 100
            val y2 = heightStep - ((data[i + 1].second - yMin) / heightRange) * heightStep + 50f
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        // Labels X
        // Donne une pair (index, pair)
        for ((i, pair) in data.withIndex()) {
            val x = i * widthStep + 50
            // .first permet de récupérer le label de la paire
            canvas.drawText(pair.first, x, height.toFloat() - 10f, textPaint)
        }

        // Labels Y
        for (i in 0..heightRange) {
            val y = heightStep - (i / heightRange.toFloat()) * heightStep + 50f
            canvas.drawText(i.toString(), 0f, y, textPaint)
        }
    }
}
