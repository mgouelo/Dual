package fr.iutvannes.dual.model.importation.readers


import fr.iutvannes.dual.model.importation.StudentDraft
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Classe de lecture de tableur d'extension .csv pour l'importation des élèves
 */
class CsvStudentReader : StudentReader {

    override fun supports(mimeType: String?, fileName: String): Boolean {
        val n = fileName.lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return n.endsWith(".csv") || mime.contains("text/csv") || mime.contains("csv")
    }

    override fun read(input: InputStream): List<StudentDraft> {

        // Lecture en UTF-8 avec gestion simple d’un éventuel BOM (caractère invisible causant un décalage)
        val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
        val lines = reader.useLines { it.toList() }.toMutableList()
        if (lines.isEmpty()) {
            return emptyList()
        }

        // En-tête
        val headerLine = stripBom(lines.removeAt(0))
        val delimiter = detectDelimiter(headerLine)
        val headerCells = parseCsvLine(headerLine, delimiter).map { normalize(it) }

        val idxFirst = findIndex(headerCells, setOf("prénom", "prenom", "first name", "firstname", "givenname"))
            ?: throw IllegalArgumentException("Colonne 'Prénom' absente dans le CSV")
        val idxLast  = findIndex(headerCells, setOf("nom", "last name", "lastname", "surname"))
            ?: throw IllegalArgumentException("Colonne 'Nom' absente dans le CSV")
        val idxClass = findIndex(headerCells, setOf("classe", "class", "group")) // optionnelle
        // Nouvelle détection pour le genre
        val idxGenre = findIndex(headerCells, setOf("genre", "sexe", "gender", "sex"))

        val out = mutableListOf<StudentDraft>()
        for (line in lines) {
            if (line.isBlank()) continue
            val cells = parseCsvLine(line, delimiter)

            val first = cells.getOrNull(idxFirst)?.trim().orEmpty()
            val last  = cells.getOrNull(idxLast )?.trim().orEmpty()
            val cls = idxClass?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            // Lecture du genre (on normalise pour avoir "M" ou "F")
            val rawGenre = idxGenre?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val finalGenre = mapToGenderCode(rawGenre)

            if (first.isBlank() && last.isBlank()) continue

            // création du brouillon
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
     * Supprimer le préfixe uFEFF afin d'éviter un décalage à la lecture
     */
    private fun stripBom(s: String): String =
        s.removePrefix("\uFEFF")

    /**
     * Détectection du délimiteur entre chaque donnée
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
     * Gestion des guillemet (ex: "val,eur" -> 1 seul champ et pas 2)
     */
    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) { // parcours de la ligne caractère par caractère
            val ch = line[i]
            when {
                ch == '"' -> { // quand le caractère est un guillemet
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // échappement "" -> "
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
     * uniformisation du texte
     */
    private fun normalize(s: String): String =
        s.trim() // retire les espaces autour
            .lowercase() // majuscule -> minuscule
            .normalizeAccents() // retire les accents
            .replace("\\s+".toRegex(), " ") // rêmplace les espaces multiples en 1

    private fun String.normalizeAccents(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")


    /**
     * Trouve l'indice des colonnes
     */
    private fun findIndex(headers: List<String>, aliases: Set<String>): Int? {
        val normalizedAliases = aliases.map { normalize(it) }.toSet()
        return headers.indexOfFirst { normalize(it) in normalizedAliases }
            .takeIf { it >= 0 }
    }

    /**
     * Convertit les entrées CSV (homme, garçon, M, femme, etc.) en "M" ou "F"
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