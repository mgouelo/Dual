package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.CourseAvecTours
import fr.iutvannes.dual.model.persistence.TourCourse

/**
 * DAO interface for the TourCourse entity.
 * It defines the data access methods for the TourCourse entity.
 *
 * @see TourCourse
 * @see CourseAvecTours
 */
@Dao
interface TourCourseDAO {

    /**
     * Inserts a new tourCourse into the database.
     *
     * @param tourCourse The tourCourse to insert.
     * @return The identifier of the inserted tourCourse.
     */
    @Insert
    suspend fun insert(tourCourse: TourCourse): Long

    /**
     * Deletes an existing tourCourse from the database.
     *
     * @param tourCourse The tourCourse to delete.
     */
    @Delete
    suspend fun delete(tourCourse: TourCourse)

    /**
     * Retrieves a tourCourse by its identifier.
     *
     * @param idTourCourse The identifier of the tourCourse to search for.
     * @return The found tourCourse
     */
    @Query("SELECT * FROM TourCourse WHERE id_course = :idCourse")
    suspend fun getTourCourseByIdCourse(idCourse: Int): List<TourCourse>

    /**
     * Updates an existing tourCourse in the database.
     *
     * @param tourCourse The tourCourse to update.
     */
    @Update
    suspend fun update(tourCourse: TourCourse)

    /**
     * Retrieves all existing tourCourses from the database.
     *
     * @return A list of tourCourses containing the tourCourses.
     */
    @Transaction
    @Query("SELECT * FROM Course WHERE id_eleve = :idEleve")
    suspend fun getAllTour(idEleve: Int): List<CourseAvecTours>
}