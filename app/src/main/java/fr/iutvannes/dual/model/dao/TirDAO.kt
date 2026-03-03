package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.SalveTir
import fr.iutvannes.dual.model.persistence.Tir

@Dao
interface TirDAO {

    @Insert
    suspend fun insert(tir: Tir): Long

    @Delete
    suspend fun delete(tir: Tir)

    @Query("SELECT * FROM Tir WHERE id_eleve = :idEleve")
    suspend fun getTirByIdEleve(idEleve: Int): Tir?

    @Query("SELECT * FROM Tir")
    suspend fun getTir(): List<Tir>

    @Update
    suspend fun update(tir: Tir)

    @Transaction
    @Query("SELECT * FROM Tir WHERE id_eleve = :idEleve")
    suspend fun getTousLesTirs(idEleve: Int): List<SalveTir>
}