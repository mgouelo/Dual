package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.SalveTir

@Dao
interface SalveTirDAO {

    @Insert
    suspend fun insert(salveTir: SalveTir): Long

    @Delete
    suspend fun delete(salveTir: SalveTir)

    @Query("SELECT * FROM SalveTir WHERE id_tir = :idTir")
    suspend fun getSalveTirByIdTir(idTir: Int): List<SalveTir>

    @Update
    suspend fun update(salveTir: SalveTir)
}