package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import java.io.InputStream


/**
 * Interface lecture des tableurs
 */
interface StudentReader {
    fun supports(mimeType: String?, fileName: String): Boolean
    fun read(input: InputStream): List<StudentDraft>
}