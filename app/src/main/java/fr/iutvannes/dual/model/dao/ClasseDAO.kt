package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
     */
    @Insert
    suspend fun insert(classe: Classe): Long

    /**
     * Deletes an existing class from the database.
     *
     * @param idClasse The identifier of the class to delete.
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
     * Retrieves all existing class names from the database.
     *
     * @return A list of strings containing the class names.
     */
    @Query("SELECT nom FROM Classe")
    suspend fun getClasses(): List<String>

    /**
     * Retrieves all existing classes from the database.
     *
     * @return A class list containing all existing classes
     */
    @Query("SELECT * FROM Classe")
    fun getAllClasses(): List<Classe>
}