package fr.iutvannes.dual.model.importation

import fr.iutvannes.dual.model.dao.ClasseDAO
import fr.iutvannes.dual.model.dao.EleveDAO
import fr.iutvannes.dual.model.importation.readers.StudentReader
import fr.iutvannes.dual.model.persistence.Classe
import fr.iutvannes.dual.model.persistence.Eleve
import java.io.InputStream

/**
 * Concise import report
 *
 * @param total number of students in the file
 * @param created number of students created
 * @param skipped number of students skipped (duplicates)
 * @param classesCreated number of classes created automatically
 * @param errorCount number of errors
 * @param errors list of errors
 */
data class ImportReport(
    val total: Int,
    val created: Int,
    val skipped: Int,
    val classesCreated: Int = 0,
    val errorCount: Int,
    val errors: List<String>
)

/**
 * Main logic of student import:
 * 1 - File type detection
 * 2 - Calling the appropriate reader
 * 3 - Data validation
 * 4 - Auto-creation of missing classes in the database
 * 5 - Batch insert of students (deduplication via unique index)
 *
 * @param readers list of known readers
 * @param eleveDao DAO of students
 * @param classeDao DAO of classes
 */
class ImportService(
    private val readers: List<StudentReader>,
    private val eleveDao: EleveDAO,
    private val classeDao: ClasseDAO
) {

    /**
     * Imports a student file.
     * Automatically creates any class found in the file that doesn't already exist.
     *
     * @param input file stream (CSV/XLS)
     * @param fileName file name (for the extension)
     * @param mimeType MIME type if known (otherwise null)
     * @param classeNom fallback class name if absent from the file
     * @return import report
     */
    suspend fun import(
        input: InputStream,
        fileName: String,
        mimeType: String? = null,
        classeNom: String?
    ): ImportReport {

        val reader = pickReader(mimeType, fileName)
        val drafts = reader.read(input)

        val errors = mutableListOf<String>()

        // ── 1. Résoudre la classe effective pour chaque draft ─────────────
        data class ResolvedDraft(
            val prenom: String,
            val nom: String,
            val genre: String,
            val classe: String,
            val rowNum: Int
        )

        val resolved = mutableListOf<ResolvedDraft>()
        drafts.forEachIndexed { idx, d ->
            val rowNum = idx + 2
            if (d.firstName.isBlank()) {
                errors += "Ligne $rowNum : 'Prénom' vide"
                return@forEachIndexed
            }
            if (d.lastName.isBlank()) {
                errors += "Ligne $rowNum : 'Nom' vide"
                return@forEachIndexed
            }
            val effectiveClasse = when {
                !d.classe.isNullOrBlank() -> d.classe.trim()
                !classeNom.isNullOrBlank() -> classeNom.trim()
                else -> "Non renseignée"
            }
            resolved += ResolvedDraft(
                prenom  = d.firstName.trim(),
                nom     = d.lastName.trim(),
                genre   = d.genre,
                classe  = effectiveClasse,
                rowNum  = rowNum
            )
        }

        // ── 2. Créer automatiquement les classes manquantes ───────────────
        val classesInFile = resolved.map { it.classe }.toSet()
        val classesExistantes = classeDao.getClasses().map { it.nom }.toSet()
        val classesACreer = classesInFile - classesExistantes

        classesACreer.forEach { nomClasse ->
            classeDao.insert(Classe(nom = nomClasse))
        }

        // ── 3. Déduplication intra-fichier ────────────────────────────────
        val seen = mutableSetOf<Triple<String, String, String>>()
        val toInsert = mutableListOf<Eleve>()
        var skipped = 0

        resolved.forEach { d ->
            val key = Triple(
                d.prenom.lowercase(),
                d.nom.lowercase(),
                d.classe.lowercase()
            )
            if (!seen.add(key)) {
                skipped++
                return@forEach
            }
            toInsert += Eleve(
                id_eleve = 0,
                prenom   = d.prenom,
                nom      = d.nom.uppercase(),
                genre    = d.genre,
                classe   = d.classe
            )
        }

        // ── 4. Insert en batch — l'index unique (nom, prenom, classe) ─────
        //    gère la déduplication base automatiquement via IGNORE
        var created = 0
        toInsert.forEach { eleve ->
            try {
                val id = eleveDao.insert(eleve)
                if (id > 0) created++ else skipped++
            } catch (e: Exception) {
                errors += "Erreur pour ${eleve.prenom} ${eleve.nom} : ${e::class.simpleName}"
            }
        }

        return ImportReport(
            total          = drafts.size,
            created        = created,
            skipped        = skipped,
            classesCreated = classesACreer.size,
            errorCount     = errors.size,
            errors         = errors
        )
    }

    private fun pickReader(mimeType: String?, fileName: String): StudentReader =
        readers.firstOrNull { it.supports(mimeType, fileName) }
            ?: throw IllegalArgumentException("Format non supporté pour '$fileName' (mime=$mimeType)")
}