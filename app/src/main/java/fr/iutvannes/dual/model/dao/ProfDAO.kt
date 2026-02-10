package fr.iutvannes.dual.model.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import fr.iutvannes.dual.model.persistence.Prof

@Dao
interface ProfDAO {

    @Insert
    suspend fun insert(prof: Prof): Long

    @Query("DELETE FROM Prof WHERE id_prof = :idProf")
    suspend fun delete(idProf: Int): Int

    @Query("SELECT * FROM Prof")
    suspend fun getAll(): List<Prof>

    @Query("SELECT * FROM Prof WHERE id_prof = :idProf")
    suspend fun getProfById(idProf: Int): Prof?

    @Query("SELECT id_prof FROM Prof")
    suspend fun getProfId(): Int

    @Query("SELECT * FROM Prof WHERE email = :email")
    suspend fun getProfByEmail(email: String): Prof?

    @Query("SELECT * FROM Prof WHERE email = :email")
    fun getProfLive(email: String): LiveData<Prof?>

    @Update
    suspend fun update(prof: Prof): Int
}
