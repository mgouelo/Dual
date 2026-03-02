package fr.iutvannes.dual.model.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Vue graphique pour afficher un graphe de points
 *
 * @param context Contexte de l'application
 * @param attrs Attributs de la vue
 */
class GraphView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /* Donnés : paire (label X, valeur Y), ex : (seance, score) */
    var data: List<Pair<String, Float>> = emptyList()

    /* Valeur des ordonnées minimale */
    var yMin: Int = 0
    /* Valeur des ordonnées maximale */
    var yMax: Int = 10

    /* Labels personnalisés pour l'axe Y */
    var yLabels: List<Float>? = null

    // Couleurs
    /* Couleur pour la ligne */
    var lineColor: Int = Color.BLUE

    /* Couleur pour les labels */
    var labelColor: Int = Color.BLACK

    /* Couleur pour la grille */
    var gridColor: Int = Color.LTGRAY

    // Paints
    /* Apparence de la ligne */
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    /* Apparence des labels */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
    }

    /* Apparence de la grille */
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    /* Apparence des points */
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /**
     * Méthode appelée lors de la mise à jour de la vue
     * Dessine le graphe
     *
     * @param canvas Canvas sur lequel on dessine
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        // Mettre à jour les couleurs
        linePaint.color = lineColor
        textPaint.color = labelColor
        gridPaint.color = gridColor

        // Distance entre de point du graphe avec une marge
        var widthStep = width.toFloat() - 200f
        if (data.size > 1) {
            widthStep /=  (data.size - 1)
        }
        val heightRange = if (yMax != yMin) yMax - yMin else 1
        val heightStep = height - 100f

        // Tracer la grille horizontale
        for (i in 0..heightRange) {
            val y = heightStep - (i / heightRange.toFloat()) * heightStep + 50f // Ajout de la marge
            canvas.drawLine(35f, y, width.toFloat(), y, gridPaint)
        }

        // Placer les points
        for ((i, pair) in data.withIndex()) {
            if (data.size == 1) {
                val x = width / 2
                val y = heightStep - ((pair.second - yMin) / heightRange) * heightStep + 50f // Ajout de la marge
                canvas.drawCircle(x.toFloat(), y, 8f, pointPaint) // rayon = 8px
            } else {
                val x = i * widthStep + 100f
                val y = heightStep - ((pair.second - yMin) / heightRange) * heightStep + 50f // Ajout de la marge
                canvas.drawCircle(x, y, 8f, pointPaint) // rayon = 8px
            }
        }

        // Tracer la ligne
        for (i in 0 until data.size - 1) {
            val x1 = i * widthStep + 100f // Ajout de la marge
            // .second permet de récupérer la valeur de la paire
            val y1 = heightStep - ((data[i].second - yMin) / heightRange) * heightStep + 50f // Ajout de la marge
            val x2 = (i + 1) * widthStep + 100f // Ajout de la marge
            val y2 = heightStep - ((data[i + 1].second - yMin) / heightRange) * heightStep + 50f // Ajout de la marge
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        // Labels X
        // Le withIndex() permet de donner une pair (index, pair)
        for ((i, pair) in data.withIndex()) {
            if (data.size == 1) {
                val text = pair.first
                val textWidth = textPaint.measureText(text)
                val x = width / 2f - textWidth / 2f
                // .first permet de récupérer le label de la paire
                canvas.drawText(pair.first, x, height.toFloat() - 10f, textPaint)
            } else {
                val text = pair.first
                val textWidth = textPaint.measureText(text)
                val x = i * widthStep - textWidth / 2f + 100f // Ajout de la marge
                canvas.drawText(pair.first, x, height.toFloat() - 10f, textPaint)
            }
        }

        // Labels Y
        if (yLabels == null) {
            for (i in 0..heightRange) {
                val y = heightStep - (i / heightRange.toFloat()) * heightStep + 50f // Ajout de la marge
                canvas.drawText(i.toString(), 0f, y, textPaint)
            }
        } else {
            val labels = yLabels ?: (yMin..yMax).map { it.toFloat() }
            val labelCount = labels.size
            val stepDivider = if (labelCount > 1) (labelCount - 1).toFloat() else 1f
            for ((i, value) in labels.withIndex()) {
                val y = heightStep - (i / stepDivider) * heightStep + 50f
                canvas.drawText(value.toString(), 0f, y, textPaint)
            }
        }
    }
}
