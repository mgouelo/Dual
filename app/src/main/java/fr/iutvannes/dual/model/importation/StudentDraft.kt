package fr.iutvannes.dual.model.importation

/**
 * Student class "draft" read from the spreadsheet before saving to the database
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