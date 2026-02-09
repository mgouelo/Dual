package fr.iutvannes.dual.model.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import fr.iutvannes.dual.model.persistence.Prof

/**
 * Interface DAO pour l'entité Prof.
 * Elle définit les méthodes d'accès aux données pour l'entité Prof.
 *
 * @see Prof
 */
@Dao
interface ProfDAO {

    /**
     * Insère un nouveau Prof dans la base de données.
     *
     * @param prof Le Prof à insérer
     */
    @Insert
    suspend fun insert(prof: Prof): Long

    /**
     * Supprime un Prof de la base de données.
     *
     * @param idProf L'identifiant du Prof à supprimer
     */
    @Query("DELETE FROM Prof WHERE id_prof = :idProf")
    suspend fun delete(idProf: Int): Int

    /**
     * Récupère tous les Prof de la base de données.
     *
     * @return Une liste de tous les Prof
     */
    @Query("SELECT * FROM Prof")
    suspend fun getAll(): List<Prof>

    /**
     * Récupère un Prof par son identifiant.
     *
     * @param idProf L'identifiant du Prof à récupérer
     * @return Le Prof
     */
    @Query("SELECT * FROM Prof WHERE id_prof = :idProf")
    suspend fun getProfById(idProf: Int): Prof?

    /**
     * Récupère un Prof par son email.
     *
     * @param email L'email du Prof à récupérer
     * @return Le Prof
     */
    @Query("SELECT * FROM Prof WHERE email = :email")
    suspend fun getProfByEmail(email: String): Prof?

    /**
     * Récupère un Prof par son email en utilisant LiveData.
     *
     * @param email L'email du Prof à récupérer
     * @return Un LiveData contenant le Prof
     */
    @Query("SELECT * FROM Prof WHERE email = :email")
    fun getProfLive(email: String): LiveData<Prof?>

    /**
     * Met à jour un Prof dans la base de données.
     *
     * @param prof Le Prof à mettre à jour
     */
    @Update
    suspend fun update(prof: Prof): Int
}
