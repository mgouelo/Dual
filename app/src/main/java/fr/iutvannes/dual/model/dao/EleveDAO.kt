package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Classe
import fr.iutvannes.dual.model.persistence.Eleve

@Dao
interface EleveDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eleve: Eleve): Long

    @Delete
    suspend fun delete(eleve: Eleve)

    @Query("SELECT * FROM Eleve")
    suspend fun getAll(): List<Eleve>

    @Query("SELECT * FROM Eleve WHERE id_eleve = :idEleve")
    suspend fun getEleveById(idEleve: Int): Eleve?

    @Update
    suspend fun update(eleve: Eleve): Int

    @Query("SELECT DISTINCT classe FROM Eleve")
    fun getClasses(): List<String>

    @Query("SELECT * FROM Eleve WHERE classe = :classe")
    fun getElevesByClasse(classe: String): List<Eleve>

    @Query("SELECT COUNT(*) FROM Eleve WHERE classe = :nomClasse")
    fun countElevesByClasse(nomClasse: String): Int

    @Query("DELETE FROM Eleve")
    fun clearTable()

    @Query("SELECT * FROM Eleve")
    fun getAllEleves(): List<Eleve>

    @Query("SELECT * FROM Eleve WHERE LOWER(prenom) = LOWER(:prenom) AND LOWER(nom) = LOWER(:nom) LIMIT 1")
    fun findByName(prenom: String, nom: String): Eleve?

    @Query("UPDATE Eleve SET classe = :nouveauNom WHERE classe = :ancienNom")
    suspend fun updateClasseEleves(ancienNom: String, nouveauNom: String)

    @Query("DELETE FROM Eleve WHERE classe = :nomClasse")
    suspend fun deleteElevesByClasse(nomClasse: String)
}