package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Reader for CSV files containing student data.
 * Supports flexible column headers including PRONOTE and STS-Web export formats.
 */
class CsvStudentReader : StudentReader {

    override fun supports(mimeType: String?, fileName: String): Boolean {
        val n = fileName.lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return n.endsWith(".csv") || mime.contains("text/csv") || mime.contains("csv")
    }

    override fun read(input: InputStream): List<StudentDraft> {
        val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
        val lines = reader.useLines { it.toList() }.toMutableList()
        if (lines.isEmpty()) return emptyList()

        val headerLine = stripBom(lines.removeAt(0))
        val delimiter = detectDelimiter(headerLine)
        val headerCells = parseCsvLine(headerLine, delimiter).map { normalize(it) }

        val idxFirst = findIndex(
            headerCells,
            setOf("prénom", "prenom", "first name", "firstname", "givenname",
                // PRONOTE
                "prénom de l'élève", "prenom de l'eleve")
        ) ?: throw IllegalArgumentException("Colonne 'Prénom' absente dans le CSV")

        val idxLast = findIndex(
            headerCells,
            setOf("nom", "last name", "lastname", "surname",
                // PRONOTE
                "nom de famille", "nom de l'élève", "nom de l'eleve")
        ) ?: throw IllegalArgumentException("Colonne 'Nom' absente dans le CSV")

        // Classe — optionnelle, nombreux alias PRONOTE / STS-Web
        val idxClass = findIndex(
            headerCells,
            setOf("classe", "class", "group", "groupe",
                // PRONOTE
                "classe actuelle", "division", "classe d'appartenance",
                // STS-Web
                "libelle division", "libellé division", "division actuelle")
        )

        val idxGenre = findIndex(
            headerCells,
            setOf("genre", "sexe", "gender", "sex",
                // PRONOTE
                "sexe de l'élève", "sexe de l'eleve")
        )

        val out = mutableListOf<StudentDraft>()
        for (line in lines) {
            if (line.isBlank()) continue
            val cells = parseCsvLine(line, delimiter)

            val first = cells.getOrNull(idxFirst)?.trim().orEmpty()
            val last  = cells.getOrNull(idxLast)?.trim().orEmpty()
            val cls   = idxClass?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            val rawGenre  = idxGenre?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val finalGenre = mapToGenderCode(rawGenre)

            if (first.isBlank() && last.isBlank()) continue

            out += StudentDraft(
                firstName = first,
                lastName  = last,
                genre     = finalGenre,
                classe    = cls.ifBlank { null }
            )
        }
        return out
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun stripBom(s: String) = s.removePrefix("\uFEFF")

    private fun detectDelimiter(header: String): Char {
        val commas = header.count { it == ',' }
        val semis  = header.count { it == ';' }
        return if (semis > commas) ';' else ','
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> { out += sb.toString(); sb.setLength(0) }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    private fun normalize(s: String): String =
        s.trim()
            .lowercase()
            .normalizeAccents()
            .replace("\\s+".toRegex(), " ")

    private fun String.normalizeAccents(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

    private fun findIndex(headers: List<String>, aliases: Set<String>): Int? {
        val normalizedAliases = aliases.map { normalize(it) }.toSet()
        return headers.indexOfFirst { normalize(it) in normalizedAliases }.takeIf { it >= 0 }
    }

    private fun mapToGenderCode(s: String): String {
        val clean = s.trim().uppercase()
        return when {
            clean.startsWith("H") || clean.startsWith("M") || clean.startsWith("G") -> "M"
            clean.startsWith("F") -> "F"
            else -> "M"
        }
    }
}