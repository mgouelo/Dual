package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Seance

/**
 * Interface DAO pour l'entité Seance.
 * Elle définit les méthodes d'accès aux données pour l'entité Seance.
 *
 * @see Seance
 */
@Dao
interface SeanceDAO {

    /**
     * Insère une nouvelle séance dans la base de données.
     *
     * @param seance La séance à insérer.
     */
    @Insert
    suspend fun insert(seance: Seance): Long

    /**
     * Supprime une séance de la base de données.
     *
     * @param idSeance L'identifiant de la séance à supprimer.
     */
    @Query("DELETE FROM Seance WHERE id_seance = :idSeance")
    suspend fun delete(idSeance: Int): Int

    /**
     * Récupère toutes les séances de la base de données.
     *
     * @return Une liste de toutes les séances de la base de données.
     */
    @Query("SELECT * FROM Seance")
    suspend fun getAll(): List<Seance>

    /**
     * Récupère une séance par son identifiant.
     *
     * @param idSeance L'identifiant de la séance à récupérer.
     * @return La séance correspondante ou null si elle n'existe pas.
     */
    @Query("SELECT * FROM Seance WHERE id_seance = :idSeance")
    suspend fun getSeanceById(idSeance: Int): Seance?

    /**
     * Met à jour une séance dans la base de données.
     *
     * @param seance La séance à mettre à jour.
     */
    @Update
    suspend fun update(seance: Seance): Int
}