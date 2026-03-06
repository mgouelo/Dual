package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import java.io.InputStream


/**
 * Spreadsheet reading interface
 *
 * @see StudentDraft
 */
interface StudentReader {

    /**
     * Checks if the file can be read by this reader
     *
     * @param mimeType the MIME type of the file
     * @param fileName the name of the file
     * @return true if the file can be read, false otherwise
     */
    fun supports(mimeType: String?, fileName: String): Boolean

    /**
     * File reading method
     *
     * @param input the input stream
     * @return the list of drafts to import
     */
    fun read(input: InputStream): List<StudentDraft>
}