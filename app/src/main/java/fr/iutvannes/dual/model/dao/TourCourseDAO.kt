package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.CourseAvecTours
import fr.iutvannes.dual.model.persistence.TourCourse

@Dao
interface TourCourseDAO {

    @Insert
    suspend fun insert(tourCourse: TourCourse): Long

    @Delete
    suspend fun delete(tourCourse: TourCourse)

    @Query("SELECT * FROM TourCourse WHERE id_course = :idCourse")
    suspend fun getTourCourseByIdCourse(idCourse: Int): List<TourCourse>

    @Update
    suspend fun update(tourCourse: TourCourse)

    @Transaction
    @Query("SELECT * FROM Course WHERE id_eleve = :idEleve")
    suspend fun getAllTour(idEleve: Int): List<CourseAvecTours>
}