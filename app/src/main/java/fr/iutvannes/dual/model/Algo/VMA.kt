package fr.iutvannes.dual.model.Algo

/**
 * Classe VMA (Vitesse Moyenne Akhir)
 * Elle permet de calculer la VMA à partir d'une distance et d'un temps donnés.
 */
class VMA {

    /**
     * Calcul la VMA à partir de la distance et du temps donnés.
     *
     * @param distance La distance parcourue en km.
     * @param temps Le temps passé en heures.
     * @return La VMA en km/h.
     * @throws IllegalArgumentException Si le temps est inférieur ou égal à zéro.
     */
    fun calculer(distance: Double, temps: Double): Double {
        if(temps <= 0.0) {
            throw IllegalArgumentException("Le temps doit être supérieur à zéro")
        }
        return distance / temps * 3.6
    }
}