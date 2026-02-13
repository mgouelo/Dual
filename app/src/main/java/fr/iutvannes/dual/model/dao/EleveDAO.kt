package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Eleve

/**
 * DAO interface for the Student entity.
 * It defines the data access methods for the Student entity.
 *
 * @see Eleve
 */
@Dao
interface EleveDAO {

    /**
     * Inserts a new student into the database.
     *
     * @param eleve The student to be inserted
     * @return The ID of the inserted student
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eleve: Eleve): Long

    /**
     * Deletes a student from the database.
     *
     * @param eleve The student to be deleted
     */
    @Delete
    suspend fun delete(eleve: Eleve)

    /**
     * Deletes a student from the database.
     *
     * @param idEleve The student ID to be deleted
     * @return The number of rows deleted
     */
    @Query("DELETE FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun delete(idEleve: Int): Int

    /**
     * Retrieves all students from the database.
     *
     * @return A list of all students
     */
    @Query("SELECT * FROM Eleve")
    suspend fun getAll(): List<Eleve>

    /**
     * Retrieves a student by their ID.
     *
     * @param idEleve The student ID to be deleted
     * @return The student corresponding to the ID
     */
    @Query("SELECT * FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun getEleveById(idEleve: Int): Eleve?

    /**
     * Updates a student in the database.
     *
     * @param eleve The student to update
     * @return The number of rows updated
     */
    @Update
    suspend fun update(eleve: Eleve): Int

    // WARNING: REVIEW WITH THE TEAM AS THIS METHOD IS USELESS
    @Query("SELECT DISTINCT classe FROM Eleve")
    fun getClasses(): List<String>

    /**
     * Retrieves all students from a specific class.
     *
     * @param classe The name of the class to search for
     * @return A list of all students in the class
     */
    @Query("SELECT * FROM Eleve WHERE classe = :classe")
    fun getElevesByClasse(classe: String): List<Eleve>

    /**
     * Retrieves the number of students in a specific class.
     *
     * @param nomClasse The name of the class to search for
     * @return The number of students in the class
     */
    @Query("SELECT COUNT(*) FROM Eleve WHERE classe = :nomClasse")
    fun countElevesByClasse(nomClasse: String): Int

    /**
     * Remove all students from the table.
     */
    @Query("DELETE FROM Eleve")
    fun clearTable()

    // WARNING: METHOD ALREADY EXISTS, THIS IS NAMED getAll
    @Query("SELECT * FROM Eleve")
    fun getAllEleves(): List<Eleve>

    /**
     * Retrieve a student by their first and last name.
     *
     * @param prenom The first name of the student to search for
     * @param nom The name of the student to search for
     * @return The student corresponding to the first and last name
     */
    @Query("SELECT * FROM Eleve WHERE LOWER(prenom) = LOWER(:prenom) AND LOWER(nom) = LOWER(:nom) LIMIT 1")
    fun findByName(prenom: String, nom: String): Eleve?

    @Query("UPDATE Eleve SET classe = :nouveauNom WHERE classe = :ancienNom")
    suspend fun updateClasseEleves(ancienNom: String, nouveauNom: String)

    @Query("DELETE FROM Eleve WHERE classe = :nomClasse")
    suspend fun deleteElevesByClasse(nomClasse: String)
}