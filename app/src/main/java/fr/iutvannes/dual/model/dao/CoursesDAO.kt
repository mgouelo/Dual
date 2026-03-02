package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Courses

@Dao
interface CoursesDAO {

    @Insert
    suspend fun insert(classe: Courses): Long

    @Delete
    suspend fun delete(classe: Courses)

    @Query("SELECT * FROM Courses WHERE id_eleve = :idEleve")
    suspend fun getCoursesByIdEleve(idEleve: Int): Courses?

    @Query("SELECT * FROM Classe")
    suspend fun getCourses(): List<Courses>

    @Update
    suspend fun update(classe: Courses)
}