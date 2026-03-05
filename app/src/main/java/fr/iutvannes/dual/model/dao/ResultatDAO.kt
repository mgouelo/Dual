package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Resultat

/**
 * DAO interface for the Result entity.
 * It defines the data access methods for the Result entity.
 *
 * @see Resultat
 */
@Dao
interface ResultatDAO {

    /**
     * Inserts a new result into the database.
     *
     * @param resultat The result to be inserted
     */
    @Insert
    suspend fun insert(resultat: Resultat): Long

    /**
     * Deletes a result from the database.
     *
     * @param idResultat The ID of the result to be deleted
     */
    @Query("DELETE FROM Resultat WHERE id_resultat = :idResultat")
    suspend fun delete(idResultat: Int): Int

    /**
     * Retrieves all results from the database.
     *
     * @return A list of all results
     */
    @Query("SELECT * FROM Resultat")
    suspend fun getAllResultats(): List<Resultat>

    /**
     * Retrieves a result by its identifier.
     *
     * @param idResultat The ID of the result to retrieve
     * @return The result corresponding to the identifier, or null if no result is found
     */
    @Query("SELECT * FROM Resultat WHERE id_resultat = :idResultat")
    suspend fun getResultatById(idResultat: Int): Resultat?

    /**
     * Updates an existing result in the database.
     *
     * @param resultat The result to be updated
     */
    @Update
    suspend fun update(resultat: Resultat): Int

    /**
     * Count the number of results in the database.
     *
     * @return The number of results
     */
    @Query("SELECT COUNT(*) FROM Resultat")
    fun getCount(): Int

    /**
     * Retrieves the results of a specific seance.
     *
     * @param idSeance The ID of the seance to retrieve results for
     * @return A list of results corresponding to the seance
     */
    @Query("SELECT * FROM Resultat WHERE id_seance = :idSeance")
    fun getResultatsBySeance(idSeance: Int): List<Resultat>

    /**
     * Retrieves the results of a specific seance.
     *
     * @param idSeance The ID of the seance to retrieve results for
     * @return A list of results
     */
    @Query("SELECT * FROM Resultat WHERE id_seance = :idSeance")
    fun getBySeance(idSeance: Int): List<Resultat>
}