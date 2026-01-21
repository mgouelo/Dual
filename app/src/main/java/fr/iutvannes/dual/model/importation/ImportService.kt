package fr.iutvannes.dual.model.importation
import fr.iutvannes.dual.model.dao.EleveDAO
import fr.iutvannes.dual.model.importation.readers.StudentReader
import fr.iutvannes.dual.model.persistence.Eleve
import java.io.InputStream


/**
 * Rapport concis d' l'import
 */
data class ImportReport(
    val total: Int,
    val created: Int,
    val skipped: Int,
    val errorCount: Int,
    val errors: List<String>
)


/**
 * Logique principale de l'import des élèves :
 * 1 - Détection du type de fichier
 * 2 - Appel du reader adéquat
 * 3 - Validation des données
 * 4 - Ajout en DB
 */
class ImportService(
    private val readers: List<StudentReader>,
    private val eleveDao: EleveDAO
) {

    /**
     * Import d’un fichier d’élèves.
     * @param input flux du fichier (CSV/XLS)
     * @param fileName nom du fichier (pour l’extension)
     * @param mimeType type MIME si connu (sinon null)
     */
    suspend fun import(
        input: InputStream,
        fileName: String,
        mimeType: String? = null,
        classeNom: String?
    ): ImportReport {

        val reader = pickReader(mimeType, fileName)
        val drafts = reader.read(input)

        // Pour éviter les doublons à l'intérieur du fichier même
        val seen = mutableSetOf<Triple<String, String, String?>>()
        var created = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        drafts.forEachIndexed { idx, d ->
            // +2 car indice 0 =  ligne 2 dans le fichier = 1er élève
            val rowNum = idx + 2

            // validation minimale du nom et prénom
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
                !classeNom.isNullOrBlank() -> classeNom.trim() // on prend la classe de l’écran
                else -> "Non renseignée"
            }

            // déduplication (supression des doublons)
            val key = Triple(
                d.firstName.trim().lowercase(),
                d.lastName.trim().lowercase(),
                effectiveClasse.lowercase()
            )

            if (!seen.add(key)) {
                skipped++
                return@forEachIndexed
            }

            // Déduplication DB (vérifie si l'élève existe déjà physiquement en base)
            // On peut faire ça proprement en cherchant dans la liste des élèves de la classe
            val existants = eleveDao.getElevesByClasse(effectiveClasse)
            val estDejaEnBase = existants.any {
                it.prenom.equals(d.firstName.trim(), ignoreCase = true) &&
                        it.nom.equals(d.lastName.trim(), ignoreCase = true)
            }

            if (estDejaEnBase) {
                skipped++
                return@forEachIndexed
            }

          // creation entité Room
            val eleve = Eleve(
                id_eleve = 0, // id généré par room
                prenom = d.firstName.trim(),
                nom = d.lastName.trim(),
                genre = d.genre, // On récupère le genre
                classe = effectiveClasse
            )

            try {
                eleveDao.insert(eleve)
                created++
            } catch (e: Exception) {
                errors += "Ligne $rowNum : erreur de persistance (${e::class.simpleName})"
            }
        }

        return ImportReport(
            total = drafts.size,
            created = created,
            skipped = skipped,
            errorCount = errors.size,
            errors = errors
        )
    }

    /**
     * Choisit le premier reader qui supporte l fichier.
     */
    private fun pickReader(mimeType: String?, fileName: String): StudentReader =
        readers.firstOrNull { it.supports(mimeType, fileName) }
            ?: throw IllegalArgumentException("Format non supporté pour '$fileName' (mime=$mimeType)")
}