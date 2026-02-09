package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Resultat

@Dao
interface ResultatDAO {

    @Insert
    suspend fun insert(resultat: Resultat): Long

    @Query("DELETE FROM Resultat WHERE id_resultat = :idResultat")
    suspend fun delete(idResultat: Int): Int

    @Query("SELECT * FROM Resultat")
    suspend fun getAllResultats(): List<Resultat>

    @Query("SELECT * FROM Resultat WHERE id_resultat = :idResultat")
    suspend fun getResultatById(idResultat: Int): Resultat?

    @Update
    suspend fun update(resultat: Resultat): Int

    @Query("SELECT COUNT(*) FROM Resultat")
    fun getCount(): Int
}