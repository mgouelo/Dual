package fr.iutvannes.dual.model.importation.readers

import fr.iutvannes.dual.model.importation.StudentDraft
import java.io.InputStream


/**
 * Interface lecture des tableurs
 *
 * @see StudentDraft
 */
interface StudentReader {

    /**
     * Vérifie si le fichier peut être lu par ce lecteur
     *
     * @param mimeType le type MIME du fichier
     * @param fileName le nom du fichier
     * @return vrai si le fichier peut être lu, faux sinon
     */
    fun supports(mimeType: String?, fileName: String): Boolean

    /**
     * Méthode de lecture du fichier
     *
     * @param input le flux d'entrée
     * @return la liste des brouillons à importer
     */
    fun read(input: InputStream): List<StudentDraft>
}