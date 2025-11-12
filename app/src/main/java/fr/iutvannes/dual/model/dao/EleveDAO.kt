package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Eleve

@Dao
interface EleveDAO {

    @Insert
    suspend fun insert(eleve: Eleve): Long

    @Query("DELETE FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun delete(idEleve: Int): Int

    @Query("SELECT * FROM Eleve")
    suspend fun getAll(): List<Eleve>

    @Query("SELECT * FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun getEleveById(idEleve: Int): Eleve?

    @Update
    suspend fun update(eleve: Eleve): Int


    @Query("SELECT DISTINCT classe FROM Eleve")
    fun getClasses(): List<String>
}