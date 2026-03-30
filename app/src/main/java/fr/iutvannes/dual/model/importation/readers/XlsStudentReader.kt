package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.Normalizer
import kotlin.math.max

/**
 * Classroom for reading spreadsheets with .xls / .xlsx (Excel) extensions for importing students
 *
 * @see StudentReader
 */
class XlsStudentReader : StudentReader {

    /**
     * Checks if the file can be read by this reader
     *
     * @param mimeType the MIME type of the file
     * @param fileName the name of the file
     * @return true if the file can be read
     */
    override fun supports(mimeType: String?, fileName: String): Boolean {
        val n = fileName.lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return n.endsWith(".xlsx") || n.endsWith(".xls")
                || mime.contains("ms-excel")
                || mime.contains("spreadsheetml")
                || mime.contains("excel") // mimeType correspondant à un tableur excel
    }

    /**
     * File reading method
     *
     * @param input the input stream
     * @return the list of drafts to import
     */
    override fun read(input: InputStream): List<StudentDraft> {
        WorkbookFactory.create(input).use { wb ->
            val sheet = wb.getSheetAt(0) ?: return emptyList()

            // Reading the header
            val headerRowIndex = sheet.firstRowNum
            val headerRow = sheet.getRow(headerRowIndex)
                ?: throw IllegalArgumentException("Erreur lecture : ligne d'en-tête introuvable ; la feuille est vide")

            val headerByIndex: Map<Int, String> = readHeader(headerRow)

            // We are looking for the columns Name and Surname
            val firstIdx = findColumnIndex(headerByIndex, setOf("prénom", "prenom", "first name", "firstname", "givenname"))
                ?: throw IllegalArgumentException("Colonne 'Prénom' inexistente")
            val lastIdx  = findColumnIndex(headerByIndex, setOf("nom", "last name", "lastname", "surname"))
                ?: throw IllegalArgumentException("Colonne 'Nom' inexistente")
            val classIdx = findColumnIndex(headerByIndex, setOf("classe", "class", "group")) // On essaye de chercher aussi une colonne classe
            // New detection for the genus
            val idxGenre = findColumnIndex(headerByIndex, setOf("genre", "sexe", "gender", "sex"))

            // We iterate through the rows associated with the columns found.
            val out = mutableListOf<StudentDraft>()
            val formatter = DataFormatter() // Renders numbers/dates as "human" text
            val lastRow = sheet.lastRowNum
            for (r in (headerRowIndex + 1)..lastRow) {
                val row = sheet.getRow(r) ?: continue

                val first = cellString(row.getCell(firstIdx), formatter).trim()
                val last  = cellString(row.getCell(lastIdx),  formatter).trim()
                val cls   = classIdx?.let { cellString(row.getCell(it), formatter).trim() }.orEmpty()

                // Gender reading (normalized to get "M" or "F")
                val rawGenre = idxGenre?.let { cellString(row.getCell(it), formatter).trim() }.orEmpty()
                val finalGenre = mapToGenderCode(rawGenre)

                // Empty lines are ignored
                if (first.isBlank() && last.isBlank()) {
                    continue
                }

                out += StudentDraft(
                    firstName = first,
                    lastName  = last,
                    genre =  finalGenre,
                    classe   = cls.ifBlank { null } // null if not found
                )
            }
            return out
        }
    }

    // --- helpers : Allows the import to be tolerant and to function in several scenarios ---
    /**
     * Read the file headers and build a lookup table
     * Column position (Int): Normalized header name (String)
     *
     * @param row the header row
     * @return the lookup table
     */
    private fun readHeader(row: Row): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        val lastCell = max(0, row.lastCellNum.toInt() - 1)
        for (c in 0..lastCell) {
            val v = row.getCell(c)?.let { rawHeader(it) } ?: continue
            if (v.isNotBlank()) {
                map[c] = v
            }
        }
        return map
    }

    /**
     * Text normalization (e.g., First Name --> first name)
     *
     * @param cell the cell to normalize
     * @return the normalized text
     */
    private fun rawHeader(cell: Cell): String =
        normalizeAscii(cell.toString())

    /**
     * Find the column position corresponding to an alias
     *
     * @param headerByIndex the lookup table
     * @param aliases the aliases to search for
     * @return the column position or null if not found
     */
    private fun findColumnIndex(headerByIndex: Map<Int, String>, aliases: Set<String>): Int? {
        val normalized = aliases.map { normalizeAscii(it) }.toSet()
        return headerByIndex.entries.firstOrNull { (_, name) -> name in normalized }?.key
    }

    /**
     * Returns the cells as a string
     *
     * @param cell the cell to convert
     * @param formatter the data formatter
     * @return the formatted string
     */
    private fun cellString(cell: Cell?, formatter: DataFormatter): String {
        if (cell == null) return ""
        // DataFormatter handles data types (numeric, date, text, etc.) and applies the Excel format.
        return formatter.formatCellValue(cell).orEmpty()
    }

    /**
     * Text normalization:
     * lowercase, no accents, standard space
     *
     * @param s the string to normalize
     * @return the normalized string
     */
    private fun normalizeAscii(s: String): String {
        // trim + lowercase + remove accents + compact spaces
        val lowered = s.trim().lowercase()
        val noAccents = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents.replace("\\s+".toRegex(), " ")
    }

    /**
     * Converts XLS entries (male, boy, M, female, etc.) to "M" or "F"
     *
     * @param s the string to convert
     * @return "M" or "F"
     */
    private fun mapToGenderCode(s: String): String {
        val clean = s.trim().uppercase()
        return when {
            clean.startsWith("H") || clean.startsWith("M") || clean.startsWith("G") -> "M"
            clean.startsWith("F") -> "F"
            else -> "M" // Default value
        }
    }
}