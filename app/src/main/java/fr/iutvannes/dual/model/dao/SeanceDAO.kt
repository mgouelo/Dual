package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Seance

/**
 * DAO interface for the Session entity.
 * It defines the data access methods for the Session entity.
 *
 * @see Seance
 */
@Dao
interface SeanceDAO {

    /**
     * Inserts a new session into the database.
     *
     * @param seance The session to be inserted.
     */
    @Insert
    suspend fun insert(seance: Seance): Long

    /**
     * Deletes a session from the database.
     *
     * @param idSeance The session ID to be deleted.
     */
    @Query("DELETE FROM Seance WHERE id_seance = :idSeance")
    suspend fun delete(idSeance: Int): Int

    /**
     * Retrieves all sessions from the database.
     *
     * @return A list of all sessions from the database.
     */
    @Query("SELECT * FROM Seance")
    suspend fun getAll(): List<Seance>

    /**
     * Retrieve a session using its identifier.
     *
     * @param idSeance The session ID to retrieve.
     * @return The corresponding session, or null if it does not exist.
     */
    @Query("SELECT * FROM Seance WHERE id_seance = :idSeance")
    suspend fun getSeanceById(idSeance: Int): Seance?

    /**
     * Updates a session in the database.
     *
     * @param seance The session needs updating.
     */
    @Update
    suspend fun update(seance: Seance): Int
}