package fr.iutvannes.dual.model.importation

/**
 * Classe élève "brouillon" lu depuis le tableur avant l'enregistrement en base de donnée
 *
 * @property firstName
 * @property lastName
 * @property genre
 * @property classe
 */
data class StudentDraft (
    val firstName: String,
    val lastName: String,
    val genre: String,
    val classe: String? = null
)