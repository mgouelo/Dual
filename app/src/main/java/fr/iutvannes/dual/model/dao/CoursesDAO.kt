package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Courses
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.model.persistence.Seance

@Dao
interface CoursesDAO {

    @Insert
    suspend fun insert(courses: Courses): Long

    @Delete
    suspend fun delete(courses: Courses)

    @Query("SELECT * FROM Courses WHERE id_eleve = :idEleve")
    suspend fun getCoursesByIdEleve(idEleve: Int): List<Courses>

    @Query("SELECT * FROM Courses")
    suspend fun getCourses(): List<Courses>

    @Update
    suspend fun update(courses: Courses)
}