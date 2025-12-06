package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import fr.iutvannes.dual.model.persistence.Classe

@Dao
interface ClasseDAO {

    @Insert
    suspend fun insert(classe: Classe): Long

    @Query("DELETE FROM Classe WHERE id_classe = :idClasse")
    suspend fun delete(idClasse: Int): Int

    //getClasseByName
    @Query("SELECT * FROM Classe WHERE nom = :nomClasse")
    suspend fun getClasseByName(nomClasse: String): Classe?

    //getClasses
    @Query("SELECT nom FROM Classe")
    suspend fun getClasses(): List<String>
}