package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import fr.iutvannes.dual.model.importation.readers.StudentReader
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.Normalizer
import kotlin.math.max

/**
 * Classe de lecture de tableur d'extension .xls / .xlsx (Excel) pour l'importation des élèves
 */
class XlsStudentReader : StudentReader {
    override fun supports(mimeType: String?, fileName: String): Boolean {
        val n = fileName.lowercase()
        val mime = mimeType?.lowercase().orEmpty()
        return n.endsWith(".xlsx") || n.endsWith(".xls")
                || mime.contains("spreadsheet")
                || mime.contains("vnd.ms-excel")
                || mime.contains("vnd.openxmlformats-officedocument.spreadsheetml.sheet") // mimeType correspondant à un tableur excel
    }

    override fun read(input: InputStream): List<StudentDraft> {
        WorkbookFactory.create(input).use { wb ->
            val sheet = wb.getSheetAt(0) ?: return emptyList()

            // lecture de l'en-tête
            val headerRowIndex = sheet.firstRowNum
            val headerRow = sheet.getRow(headerRowIndex)
                ?: throw IllegalArgumentException("Erreur lecture : ligne d'en-tête introuvable ; la feuille est vide")

            val headerByIndex: Map<Int, String> = readHeader(headerRow)

            // On cherche les colonnes Nom et Prénom
            val firstIdx = findColumnIndex(headerByIndex, setOf("prénom", "prenom", "first name", "firstname", "givenname"))
                ?: throw IllegalArgumentException("Colonne 'Prénom' inexistente")
            val lastIdx  = findColumnIndex(headerByIndex, setOf("nom", "last name", "lastname", "surname"))
                ?: throw IllegalArgumentException("Colonne 'Nom' inexistente")
            val classIdx = findColumnIndex(headerByIndex, setOf("classe", "class", "group")) // On essaye de chercher aussi une colonne classe

            // On parcours les lignes associées aux colones trouvées
            val out = mutableListOf<StudentDraft>()
            val formatter = DataFormatter() // rend les nombres/dates en texte "humain"
            val lastRow = sheet.lastRowNum
            for (r in (headerRowIndex + 1)..lastRow) {
                val row = sheet.getRow(r) ?: continue

                val first = cellString(row.getCell(firstIdx), formatter).trim()
                val last  = cellString(row.getCell(lastIdx),  formatter).trim()
                val cls   = classIdx?.let { cellString(row.getCell(it), formatter).trim() }.orEmpty()

                // on ignore les lignes vides
                if (first.isBlank() && last.isBlank()) {
                    continue
                }

                out += StudentDraft(
                    firstName = first,
                    lastName  = last,
                    genre =  "", // le genre n'est pas géré dans les XLS pour l'instant
                    classe   = cls.ifBlank { null } // null si non trouvé
                )
            }
            return out
        }
    }

    // --- helpers : Permettent à l'import d'être tolérant et de fonctionner dans plusieurs cas de figure ---
    /**
     * Lis les en-tête du fichier et construit une table de correspondance
     * Position de la colonne (Int) : nom de l'en-tête normalisé (String)
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
     * Normalisation du texte (ex : Prénom --> prenom)
     */
    private fun rawHeader(cell: Cell): String =
        normalizeAscii(cell.toString())

    /**
     * Trouver la position de colonne correspondant à un alias
     */
    private fun findColumnIndex(headerByIndex: Map<Int, String>, aliases: Set<String>): Int? {
        val normalized = aliases.map { normalizeAscii(it) }.toSet()
        return headerByIndex.entries.firstOrNull { (_, name) -> name in normalized }?.key
    }

    /**
     * Renvoie les cellules en chaine de caractère
     */
    private fun cellString(cell: Cell?, formatter: DataFormatter): String {
        if (cell == null) return ""
        // DataFormatter gère les types (numérique, date, texte…) et applique le format Excel
        return formatter.formatCellValue(cell).orEmpty()
    }

    /**
     * Normalisation du texte :
     * minuscule, sans accent, espace classique
     */
    private fun normalizeAscii(s: String): String {
        // trim + lowercase + enlever accents + compacter les espaces
        val lowered = s.trim().lowercase()
        val noAccents = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents.replace("\\s+".toRegex(), " ")
    }
}