package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Course
import fr.iutvannes.dual.model.persistence.CourseAvecTours

@Dao
interface CourseDAO {

    @Insert
    suspend fun insert(classe: Course): Long

    @Delete
    suspend fun delete(classe: Course)

    @Query("SELECT * FROM Course WHERE id_eleve = :idEleve")
    suspend fun getCourseByIdEleve(idEleve: Int): Course?

    @Update
    suspend fun update(classe: Course)

    @Transaction
    @Query("SELECT * FROM Course WHERE id_eleve = :idEleve")
    suspend fun getAllTour(idEleve: Int): List<CourseAvecTours>
}