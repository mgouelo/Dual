package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.SalveTir
import fr.iutvannes.dual.model.persistence.Tir
import fr.iutvannes.dual.model.persistence.TirAvecPassages

/**
 * DAO interface for the Tir entity.
 * It defines the data access methods for the Tir entity.
 *
 * @see Tir
 */
@Dao
interface TirDAO {

    /**
     * Inserts a new tir into the database.
     *
     * @param tir The tir to insert.
     * @return The identifier of the inserted tir.
     */
    @Insert
    suspend fun insert(tir: Tir): Long

    /**
     * Deletes an existing tir from the database.
     *
     * @param tir The tir to delete.
     */
    @Delete
    suspend fun delete(tir: Tir)

    /**
     * Retrieves a tir by its identifier.
     *
     * @param idEleve The identifier of the tir to search for.
     * @return The found tir
     */
    @Query("SELECT * FROM Tir WHERE id_eleve = :idEleve")
    suspend fun getTirByIdEleve(idEleve: Int): Tir?

    /**
     * Retrieves all existing tirs from the database.
     *
     * @return A list of tir containing the tirs.
     */
    @Query("SELECT * FROM Tir")
    suspend fun getTir(): List<Tir>

    /**
     * Updates an existing tir in the database.
     *
     * @param tir The tir to update.
     */
    @Update
    suspend fun update(tir: Tir)

    /**
     * Retrieves all existing tirs from the database.
     *
     * @return A list of tir containing the tirs.
     */
    @Transaction
    @Query("SELECT * FROM Tir WHERE id_eleve = :idEleve")
    suspend fun getTousLesTirs(idEleve: Int): List<TirAvecPassages>
}