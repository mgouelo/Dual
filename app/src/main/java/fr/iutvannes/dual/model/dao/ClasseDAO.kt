package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Classe

@Dao
interface ClasseDAO {

    @Insert
    suspend fun insert(classe: Classe): Long

    @Delete
    suspend fun delete(classe: Classe)

    @Query("SELECT * FROM Classe WHERE nom = :nomClasse")
    suspend fun getClasseByName(nomClasse: String): Classe?

    @Query("SELECT * FROM Classe")
    suspend fun getClasses(): List<Classe>

    @Query("SELECT * FROM Classe")
    fun getAllClasses(): List<Classe>

    @Update
    suspend fun update(classe: Classe)

    @Query("UPDATE Classe SET nom = :nouveauNom WHERE nom = :ancienNom")
    suspend fun updateNomClasse(ancienNom: String, nouveauNom: String)

    @Query("SELECT nom FROM Classe") 
    fun getAllNames(): List<String>
}