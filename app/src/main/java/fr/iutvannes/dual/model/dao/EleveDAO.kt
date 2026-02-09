package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Eleve

/**
 * Interface DAO pour l'entité Eleve.
 * Elle définit les méthodes d'accès aux données pour l'entité Eleve.
 *
 * @see Eleve
 */
@Dao
interface EleveDAO {

    /**
     * Insère un nouveau élève dans la base de données.
     *
     * @param eleve L'élève à insérer
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eleve: Eleve): Long

    /**
     * Supprime un élève de la base de données.
     *
     * @param idEleve L'ID de l'élève à supprimer
     */
    @Query("DELETE FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun delete(idEleve: Int): Int

    /**
     * Récupère tous les élèves de la base de données.
     *
     * @return Une liste de tous les élèves
     */
    @Query("SELECT * FROM Eleve")
    suspend fun getAll(): List<Eleve>

    /**
     * Récupère un élève par son ID.
     *
     * @param idEleve L'ID de l'élève à récupérer
     * @return L'élève correspondant à l'ID
     */
    @Query("SELECT * FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun getEleveById(idEleve: Int): Eleve?

    /**
     * Met à jour un élève dans la base de données.
     *
     * @param eleve L'élève à mettre à jour
     */
    @Update
    suspend fun update(eleve: Eleve): Int

    //WARNING : A REVOIR AVEC L'EQUIPE CAR METHODE INNUTILE
    @Query("SELECT DISTINCT classe FROM Eleve")
    fun getClasses(): List<String>

    /**
     * Récupère tous les élèves d'une classe spécifique.
     *
     * @param classe Le nom de la classe à rechercher
     */
    @Query("SELECT * FROM Eleve WHERE classe = :classe")
    fun getElevesByClasse(classe: String): List<Eleve>

    /**
     * Supprime tous les élèves de la table.
     */
    @Query("DELETE FROM Eleve")
    fun clearTable()

    //WARNING METHODE DEJA EXISTANT CE NOMMANT getAll
    @Query("SELECT * FROM Eleve")
    fun getAllEleves(): List<Eleve>

    /**
     * Récupère un élève par son prénom et nom.
     *
     * @param prenom Le prénom de l'élève à rechercher
     * @param nom Le nom de l'élève à rechercher
     * @return L'élève correspondant au prénom et nom
     */
    @Query("SELECT * FROM Eleve WHERE LOWER(prenom) = LOWER(:prenom) AND LOWER(nom) = LOWER(:nom) LIMIT 1")
    fun findByName(prenom: String, nom: String): Eleve?
}