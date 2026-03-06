package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Course
import fr.iutvannes.dual.model.persistence.CourseAvecTours

/**
 * DAO interface for the Course entity.
 * It defines the data access methods for the Course entity.
 *
 * @see Course
 */
@Dao
interface CourseDAO {

    /**
     * Inserts a new course into the database.
     *
     * @param classe The course to insert.
     * @return The identifier of the inserted course.
     */
    @Insert
    suspend fun insert(course: Course): Long

    /**
     * Deletes an existing course from the database.
     *
     * @param course The course to delete.
     */
    @Delete
    suspend fun delete(course: Course)

    /**
     * Retrieves a course by its identifier.
     *
     * @param idCourse The identifier of the course to search for.
     * @return The found course
     */
    @Query("SELECT * FROM Course WHERE id_eleve = :idEleve")
    suspend fun getCourseByIdEleve(idEleve: Int): Course?

    /**
     * Updates an existing course in the database.
     *
     * @param course The course to update.
     */
    @Update
    suspend fun update(course: Course)

    /**
     * Retrieves all existing courses from the database.
     *
     * @return A list of courses containing the courses.
     */
    @Transaction
    @Query("SELECT * FROM Course WHERE id_eleve = :idEleve")
    suspend fun getAllTour(idEleve: Int): List<CourseAvecTours>
}