package fr.iutvannes.dual.model.importation

/**
 * Classe élève "brouillon" lu depuis le tableur avant l'enregistrement en base de donnée
 *
 */
data class StudentDraft (
    val firstName: String,
    val lastName: String,
    val classe: String? = null
)