package fr.iutvannes.dual.model.importation.readers


import fr.iutvannes.dual.model.importation.StudentDraft
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Classroom for reading spreadsheets with .csv extension for importing students
 */
class CsvStudentReader : StudentReader {

    /**
     * Checks if the file can be read by this reader
     *
     * @param mimeType the MIME type of the file
     * @param fileName the name of the file
     * @return true if the file can be read by this reader
     */
    override fun supports(mimeType: String?, fileName: String): Boolean {
        val n = fileName.lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return n.endsWith(".csv") || mime.contains("text/csv") || mime.contains("csv")
    }

    /**
     * File reading method
     *
     * @param input the input stream
     * @return the list of drafts to import
     * @throws IllegalArgumentException if the file cannot be read
     */
    override fun read(input: InputStream): List<StudentDraft> {

        // UTF-8 encoding with simple handling of any BOM (invisible character causing a shift)
        val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
        val lines = reader.useLines { it.toList() }.toMutableList()
        if (lines.isEmpty()) {
            return emptyList()
        }

        // Header
        val headerLine = stripBom(lines.removeAt(0))
        val delimiter = detectDelimiter(headerLine)
        val headerCells = parseCsvLine(headerLine, delimiter).map { normalize(it) }

        val idxFirst = findIndex(headerCells, setOf("prénom", "prenom", "first name", "firstname", "givenname"))
            ?: throw IllegalArgumentException("Colonne 'Prénom' absente dans le CSV")
        val idxLast  = findIndex(headerCells, setOf("nom", "last name", "lastname", "surname"))
            ?: throw IllegalArgumentException("Colonne 'Nom' absente dans le CSV")
        val idxClass = findIndex(headerCells, setOf("classe", "class", "group")) // optionnelle
        // New detection for the genus
        val idxGenre = findIndex(headerCells, setOf("genre", "sexe", "gender", "sex"))

        val out = mutableListOf<StudentDraft>()
        for (line in lines) {
            if (line.isBlank()) continue
            // Reading the line
            val cells = parseCsvLine(line, delimiter)

            val first = cells.getOrNull(idxFirst)?.trim().orEmpty()
            val last  = cells.getOrNull(idxLast )?.trim().orEmpty()
            val cls = idxClass?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            // Gender reading (normalized to get "M" or "F")
            val rawGenre = idxGenre?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val finalGenre = mapToGenderCode(rawGenre)

            if (first.isBlank() && last.isBlank()) continue

            // Creating the draft
            out += StudentDraft(
                firstName = first,
                lastName  = last,
                genre = finalGenre,
                classe   = cls.ifBlank { null }
            )
        }
        return out
    }

    // --- helpers ---

    /**
     * Remove the uFEFF prefix to avoid a reading delay
     *
     * @param s the string to clean
     * @return the cleaned string
     */
    private fun stripBom(s: String): String =
        s.removePrefix("\uFEFF")

    /**
     * Detecting the delimiter between each data element
     *
     * @param header the header row
     * @return the detected delimiter
     */
    private fun detectDelimiter(header: String): Char {
        // le + plus fréquent entre ',' et ';'
        val commas = header.count { it == ',' }
        val semis  = header.count { it == ';' }
        if (semis > commas) {
            return ';'
        } else {
            return ','
        }
    }

    /**
     * Handling quotation marks (e.g., "value" -> only one field, not two)
     *
     * @param line: the line to parse
     * @param delimiter: the delimiter
     * @return: the list of separated fields
     */
    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) { // Traversing the line character by character
            val ch = line[i]
            when {
                ch == '"' -> { // When the character is a quotation mark
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Exhaust "" -> "
                        sb.append('"');
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    out += sb.toString()
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    /**
     * Text standardization
     *
     * @param s the string to normalize
     * @return the normalized string
     */
    private fun normalize(s: String): String =
        s.trim() // Remove the spaces around
            .lowercase() // uppercase -> lowercase
            .normalizeAccents() // Remove the accents
            .replace("\\s+".toRegex(), " ") // Replaces multiple spaces in 1

    /**
     * Removes accents from a string
     *
     * @return the string without accents
     */
    private fun String.normalizeAccents(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")


    /**
     * Find the column index
     *
     * @param headers the headers
     * @param aliases the aliases
     * @return the column index
     */
    private fun findIndex(headers: List<String>, aliases: Set<String>): Int? {
        val normalizedAliases = aliases.map { normalize(it) }.toSet()
        return headers.indexOfFirst { normalize(it) in normalizedAliases }
            .takeIf { it >= 0 }
    }

    /**
     * Converts CSV inputs (male, boy, M, female, etc.) to "M" or "F"
     *
     * @param s the string to convert
     * @return "M" or "F"
     */
    private fun mapToGenderCode(s: String): String {
        val clean = s.trim().uppercase()
        return when {
            clean.startsWith("H") || clean.startsWith("M") || clean.startsWith("G") -> "M"
            clean.startsWith("F") -> "F"
            else -> "M" // Valeur par défaut
        }
    }
}