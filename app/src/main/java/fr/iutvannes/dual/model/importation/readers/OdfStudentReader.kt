package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import org.odftoolkit.simple.SpreadsheetDocument
import java.io.InputStream
import kotlin.math.max

/**
 * Classe de lecture de tableur d'extension .odf (libre office) pour l'importation des élèves
 */
class OdfStudentReader : StudentReader {

    override fun supports(mimeType: String?, fileName: String): Boolean {
        val n = fileName.lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return n.endsWith(".ods") || mime.contains("vnd.oasis.opendocument.spreadsheet")
    }

    override fun read(input: InputStream): List<StudentDraft> {
        val doc = SpreadsheetDocument.loadDocument(input)
        doc.use { d ->
            val sheet = d.getSheetByIndex(0)
                ?: return emptyList()

            // En-tête (ligne 0)
            val colCount = max(0, sheet.columnCount - 1) // simple API calcule souvent bien
            val header = mutableMapOf<Int, String>()
            for (c in 0..colCount) {
                val cell = sheet.getCellByPosition(c, 0)
                val name = normalize(cell.displayText ?: "")
                if (name.isNotBlank()) header[c] = name
            }

            val idxFirst = findIndex(header, setOf("prénom", "prenom", "first name", "firstname", "givenname"))
                ?: throw IllegalArgumentException("Colonne 'Prénom' absente dans l’ODS")
            val idxLast  = findIndex(header, setOf("nom", "last name", "lastname", "surname"))
                ?: throw IllegalArgumentException("Colonne 'Nom' absente dans l’ODS")
            val idxClass = findIndex(header, setOf("classe", "class", "group"))

            val out = mutableListOf<StudentDraft>()
            val rowCount = sheet.rowCount
            for (r in 1 until rowCount) {
                val first = sheet.getCellByPosition(idxFirst, r).displayText?.trim().orEmpty()
                val last  = sheet.getCellByPosition(idxLast,  r).displayText?.trim().orEmpty()
                val cls   = idxClass?.let { sheet.getCellByPosition(it, r).displayText?.trim().orEmpty() }.orEmpty()

                if (first.isBlank() && last.isBlank()) {
                    continue
                }

                out += StudentDraft(
                    firstName = first,
                    lastName  = last,
                    genre =  "", // le genre n'est pas géré dans les ODF pour l'instant
                    classe   = cls.ifBlank { null }
                )
            }
            return out
        }
    }

    // --- helpers ---
    /**
     * Trouve l'indice des colonnes
     */
    private fun findIndex(headerByIndex: Map<Int, String>, aliases: Set<String>): Int? {
        val normalizedAliases = aliases.map { normalize(it) }.toSet()
        return headerByIndex.entries.firstOrNull { (_, name) -> name in normalizedAliases }?.key
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
}