package fr.iutvannes.dual.model.Algo

/**
 * VMA Class (Average Speed ​​Akhir)
 * It allows you to calculate your VMA based on a given distance and time.
 */
class VMA {

    /**
     * Calculate the VMA (Maximum Aerobic Speed) from the given distance and time.
     *
     * @param distance The distance traveled in km.
     * @param temps The time elapsed in hours.
     * @return The VMA in km/h.
     * @throws IllegalArgumentException If the time is less than or equal to zero.
     */
    fun calculer(distance: Double, temps: Double): Double {
        if(temps <= 0.0) {
            throw IllegalArgumentException("Le temps doit être supérieur à zéro")
        }
        return distance / temps * 3.6
    }
}