package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.iutvannes.dual.model.persistence.Tirs

@Dao
interface TirsDAO {

    @Insert
    suspend fun insert(tirs: Tirs): Long

    @Delete
    suspend fun delete(tirs: Tirs)

    @Query("SELECT * FROM Tirs WHERE id_eleve = :idEleve")
    suspend fun getTirsByIdEleve(idEleve: Int): Tirs?

    @Query("SELECT * FROM Tirs")
    suspend fun getTirs(): List<Tirs>

    @Update
    suspend fun update(tirs: Tirs)
}