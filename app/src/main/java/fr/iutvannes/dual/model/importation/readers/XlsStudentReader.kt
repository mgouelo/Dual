package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.Normalizer
import kotlin.math.max

/**
 * Classe de lecture de tableur d'extension .xls / .xlsx (Excel) pour l'importation des élèves
 *
 * @see StudentReader
 */
class XlsStudentReader : StudentReader {

    /**
     * Vérifie si le fichier peut être lu par ce lecteur
     *
     * @param mimeType le type MIME du fichier
     * @param fileName le nom du fichier
     * @return vrai si le fichier peut être lu
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
     * Méthode de lecture du fichier
     *
     * @param input le flux d'entrée
     * @return la liste des brouillons à importer
     */
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
            // Nouvelle détection pour le genre
            val idxGenre = findColumnIndex(headerByIndex, setOf("genre", "sexe", "gender", "sex"))

            // On parcours les lignes associées aux colones trouvées
            val out = mutableListOf<StudentDraft>()
            val formatter = DataFormatter() // rend les nombres/dates en texte "humain"
            val lastRow = sheet.lastRowNum
            for (r in (headerRowIndex + 1)..lastRow) {
                val row = sheet.getRow(r) ?: continue

                val first = cellString(row.getCell(firstIdx), formatter).trim()
                val last  = cellString(row.getCell(lastIdx),  formatter).trim()
                val cls   = classIdx?.let { cellString(row.getCell(it), formatter).trim() }.orEmpty()

                // Lecture du genre (on normalise pour avoir "M" ou "F")
                val rawGenre = idxGenre?.let { cellString(row.getCell(it), formatter).trim() }.orEmpty()
                val finalGenre = mapToGenderCode(rawGenre)

                // on ignore les lignes vides
                if (first.isBlank() && last.isBlank()) {
                    continue
                }

                out += StudentDraft(
                    firstName = first,
                    lastName  = last,
                    genre =  finalGenre,
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
     *
     * @param row la ligne d'en-tête
     * @return la table de correspondance
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
     *
     * @param cell la cellule à normaliser
     * @return le texte normalisé
     */
    private fun rawHeader(cell: Cell): String =
        normalizeAscii(cell.toString())

    /**
     * Trouver la position de colonne correspondant à un alias
     *
     * @param headerByIndex la table de correspondance
     * @param aliases les aliases à rechercher
     * @return la position de la colonne ou null si pas trouvé
     */
    private fun findColumnIndex(headerByIndex: Map<Int, String>, aliases: Set<String>): Int? {
        val normalized = aliases.map { normalizeAscii(it) }.toSet()
        return headerByIndex.entries.firstOrNull { (_, name) -> name in normalized }?.key
    }

    /**
     * Renvoie les cellules en chaine de caractère
     *
     * @param cell la cellule à convertir
     * @param formatter le formatteur de données
     * @return la chaine de caractère formattée
     */
    private fun cellString(cell: Cell?, formatter: DataFormatter): String {
        if (cell == null) return ""
        // DataFormatter gère les types (numérique, date, texte…) et applique le format Excel
        return formatter.formatCellValue(cell).orEmpty()
    }

    /**
     * Normalisation du texte :
     * minuscule, sans accent, espace classique
     *
     * @param s la chaine à normaliser
     * @return la chaine normalisée
     */
    private fun normalizeAscii(s: String): String {
        // trim + lowercase + enlever accents + compacter les espaces
        val lowered = s.trim().lowercase()
        val noAccents = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents.replace("\\s+".toRegex(), " ")
    }

    /**
     * Convertit les entrées Xls (homme, garçon, M, femme, etc.) en "M" ou "F"
     *
     * @param s la chaine à convertir
     * @return "M" ou "F"
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