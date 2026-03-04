package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Classe

/**
 * DAO interface for the Class entity.
 * It defines the data access methods for the Class entity.
 *
 * @see Classe
 */
@Dao
interface ClasseDAO {


    /**
     * Inserts a new class into the database.
     *
     * @param classe The class to insert.
     * @return The identifier of the inserted class.
     */
    @Insert
    suspend fun insert(classe: Classe): Long

    /**
     * Deletes an existing class from the database.
     *
     * @param classe The class to delete.
     */
    @Delete
    suspend fun delete(classe: Classe)

    /**
     * Deletes an existing class from the database.
     *
     * @param idClasse The identifier of the class to delete.
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM Classe WHERE id_classe = :idClasse")
    suspend fun delete(idClasse: Int): Int

    /**
     * Retrieves a class by its name.
     *
     * @param nomClasse The name of the class to search for.
     * @return The found class, or null if no class is found.
     */
    @Query("SELECT * FROM Classe WHERE nom = :nomClasse")
    suspend fun getClasseByName(nomClasse: String): Classe?

    /**
     * Retrieves all existing classes from the database.
     *
     * @return A list of classes containing the classes.
     */
    @Query("SELECT * FROM Classe")
    suspend fun getClasses(): List<Classe>


    // WARNING: METHOD ALREADY EXISTS, THIS IS NAMED getAll
    /**
     * Retrieves all existing classes from the database.
     *
     * @return A class list containing all existing classes
     */
    @Query("SELECT * FROM Classe")
    fun getAllClasses(): List<Classe>

    /**
     * Updates an existing class in the database.
     *
     * @param classe The class to update.
     */
    @Update
    suspend fun update(classe: Classe)

    /**
     * Updates the name of a class in the database.
     *
     * @param ancienNom The old name of the class.
     * @param nouveauNom The new name of the class.
     */
    @Query("UPDATE Classe SET nom = :nouveauNom WHERE nom = :ancienNom")
    suspend fun updateNomClasse(ancienNom: String, nouveauNom: String)

    @Query("SELECT nom FROM Classe")
    suspend fun getAllNames(): List<String>
}