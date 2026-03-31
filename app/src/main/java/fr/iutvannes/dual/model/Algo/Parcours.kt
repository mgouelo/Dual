package fr.iutvannes.dual.model.Algo

class Parcours {
    /**
     * Détermine le parcours de l'élève en fonction de sa VMA (float)
     */
    fun determinerParcours(vma: Float): String {
        return when {
            vma < 10.5f -> "Coupelles jaunes"
            vma < 11.5f -> "Plots verts"
            vma < 12.5f -> "Coupelles bleues"
            vma < 13.5f -> "Plots bleus"
            vma < 14.5f -> "Coupelles rouges"
            vma <= 15.0f -> "Plots rouges"
            else -> "Grand tour"
        }
    }
}