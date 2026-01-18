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
        // on copie le flux dans un fichier temporaire physique + utilise createTempFile dans le cache de l'appli
        val tempFile = java.io.File.createTempFile("import_eleve", ".ods")
        tempFile.deleteOnExit() // nettoyage auto à la fin du programme

        // copie du flux vers le fichier
        input.use { stream ->
            tempFile.outputStream().use { fileOut ->
                stream.copyTo(fileOut)
            }
        }

        // charge le document depuis le fichier, pas le flux du .ods (car un .ods est un zip de plusieurs fichiers)
        val doc = SpreadsheetDocument.loadDocument(tempFile)

        doc.use { d ->
            val sheet = d.getSheetByIndex(0) ?: return emptyList()

            val colCount = max(0, sheet.columnCount - 1)
            val header = mutableMapOf<Int, String>()

            // parsing des en-têtes
            for (c in 0..colCount) {
                val cell = sheet.getCellByPosition(c, 0)
                val name = normalize(cell.displayText ?: "")
                if (name.isNotBlank()) header[c] = name
            }

            val idxFirst = findIndex(header, setOf("prénom", "prenom", "first name", "firstname"))
                ?: throw IllegalArgumentException("Colonne 'Prénom' introuvable")
            val idxLast  = findIndex(header, setOf("nom", "last name", "lastname"))
                ?: throw IllegalArgumentException("Colonne 'Nom' introuvable")
            val idxClass = findIndex(header, setOf("classe", "groupe", "class", "group"))

            val out = mutableListOf<StudentDraft>()
            val rowCount = sheet.rowCount

            // ajout d'une sécurité pour sauter les lignes vide
            for (r in 1 until rowCount) {
                val cellFirst = sheet.getCellByPosition(idxFirst, r)
                val cellLast  = sheet.getCellByPosition(idxLast, r)

                // sécurité si cellule null
                val first = cellFirst?.displayText?.trim().orEmpty()
                val last  = cellLast?.displayText?.trim().orEmpty()

                val cls = if (idxClass != null) {
                    sheet.getCellByPosition(idxClass, r)?.displayText?.trim().orEmpty()
                } else {
                    ""
                }

                if (first.isBlank() && last.isBlank()) continue

                out += StudentDraft(first, last, cls.ifBlank { null })
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