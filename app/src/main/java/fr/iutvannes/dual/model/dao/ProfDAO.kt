package fr.iutvannes.dual.model.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import fr.iutvannes.dual.model.persistence.Prof

/**
 * DAO interface for the Prof entity.
 * It defines the data access methods for the Prof entity.
 *
 * @see Prof
 */
@Dao
interface ProfDAO {

    /**
     * Inserts a new Professor into the database.
     *
     * @param prof The Professor to Insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(prof: Prof): Long

    /**
     * Delete a Professor from the database.
     *
     * @param idProf The teacher's ID to be deleted
     */
    @Query("DELETE FROM Prof WHERE id_prof = :idProf")
    suspend fun delete(idProf: Int): Int

    /**
     * Retrieves all the Profs from the database.
     *
     * @return A list of all the Professors
     */
    @Query("SELECT * FROM Prof")
    suspend fun getAll(): List<Prof>

    /**
     * Retrieve a teacher by their ID.
     *
     * @param idProf The teacher's ID to retrieve
     * @return The Prof
     */
    @Query("SELECT * FROM Prof WHERE id_prof = :idProf")
    suspend fun getProfById(idProf: Int): Prof?

    /**
     * Retrieve the teacher's ID.
     *
     * @return The teacher's ID
     */
    @Query("SELECT id_prof FROM Prof")
    suspend fun getProfId(): Int

    /**
     * Retrieve a teacher via their email.
     *
     * @param email The Professor's email to retrieve
     * @return The Prof
     */
    @Query("SELECT * FROM Prof WHERE email = :email")
    suspend fun getProfByEmail(email: String): Prof?

    /**
     * Retrieve a teacher via their email using LiveData.
     *
     * @param email The Professor's email to retrieve
     * @return A LiveData containing the Prof
     */
    @Query("SELECT * FROM Prof WHERE email = :email")
    fun getProfLive(email: String): LiveData<Prof?>

    /**
     * Retrieve the teacher's email.
     *
     * @return The teacher's email
     */
    @Query("SELECT email FROM Prof")
    suspend fun getProfEmail(): String

    /**
     * Updates a Professor in the database.
     *
     * @param prof The Professor needs updating
     */
    @Update
    suspend fun update(prof: Prof): Int
}
