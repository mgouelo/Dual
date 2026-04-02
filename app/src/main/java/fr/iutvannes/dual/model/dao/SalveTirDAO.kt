package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.SalveTir

/**
 * DAO interface for the SalveTir entity.
 * It defines the data access methods for the SalveTir entity.
 *
 * @see SalveTir
 */
@Dao
interface SalveTirDAO {

    /**
     * Inserts a new SalveTir into the database.
     *
     * @param salveTir The SalveTir to insert.
     * @return The identifier of the inserted SalveTir.
     */
    @Insert
    suspend fun insert(salveTir: SalveTir): Long

    /**
     * Deletes an existing SalveTir from the database.
     *
     * @param salveTir The SalveTir to delete.
     * @return The number of rows deleted.
     */
    @Delete
    suspend fun delete(salveTir: SalveTir)

    /**
     * Retrieves a SalveTir by its identifier.
     *
     * @param idTir The identifier of the SalveTir to search for.
     * @return The found SalveTir
     */
    @Query("SELECT * FROM SalveTir WHERE id_tir = :idTir")
    suspend fun getSalveTirByIdTir(idTir: Int): List<SalveTir>

    /**
     * Updates an existing SalveTir in the database.
     *
     * @param salveTir The SalveTir to update.
     */
    @Update
    suspend fun update(salveTir: SalveTir)
}