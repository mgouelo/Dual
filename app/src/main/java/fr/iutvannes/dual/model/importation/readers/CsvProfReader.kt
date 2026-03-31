package fr.iutvannes.dual.controller.fragments

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Draft d'un professeur lu depuis un fichier CSV,
 * avant persistance en base de données.
 */
data class ProfDraft(
    val prenom: String,
    val nom: String,
    val email: String,
    val password: String
)

/**
 * Lecteur CSV pour l'import de professeurs.
 * Même logique que [CsvStudentReader] :
 * - détection automatique du délimiteur (virgule ou point-virgule)
 * - gestion du BOM UTF-8
 * - colonnes repérées par leur en-tête (insensible à la casse et aux accents)
 *
 * Format attendu (ordre des colonnes flexible) :
 *   prenom ; nom ; email ; password
 */
class CsvProfReader {

    fun read(input: InputStream): List<ProfDraft> {
        val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
        val lines = reader.useLines { it.toList() }.toMutableList()
        if (lines.isEmpty()) return emptyList()

        val headerLine = stripBom(lines.removeAt(0))
        val delimiter  = detectDelimiter(headerLine)
        val headers    = parseCsvLine(headerLine, delimiter).map { normalize(it) }

        val idxPrenom   = findIndex(headers, setOf("prénom", "prenom", "firstname", "first name", "givenname"))
            ?: throw IllegalArgumentException("Colonne 'Prénom' absente dans le CSV")
        val idxNom      = findIndex(headers, setOf("nom", "lastname", "last name", "surname"))
            ?: throw IllegalArgumentException("Colonne 'Nom' absente dans le CSV")
        val idxEmail    = findIndex(headers, setOf("email", "mail", "courriel"))
            ?: throw IllegalArgumentException("Colonne 'Email' absente dans le CSV")
        val idxPassword = findIndex(headers, setOf("password", "motdepasse", "mot de passe", "mdp"))
            ?: throw IllegalArgumentException("Colonne 'Password' absente dans le CSV")

        return lines
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val cells = parseCsvLine(line, delimiter)
                val prenom   = cells.getOrNull(idxPrenom)?.trim().orEmpty()
                val nom      = cells.getOrNull(idxNom)?.trim().orEmpty()
                val email    = cells.getOrNull(idxEmail)?.trim().orEmpty()
                val password = cells.getOrNull(idxPassword)?.trim().orEmpty()
                if (prenom.isBlank() && nom.isBlank()) null
                else ProfDraft(prenom = prenom, nom = nom, email = email, password = password)
            }
    }

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
}