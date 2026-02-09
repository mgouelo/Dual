package fr.iutvannes.dual.model.dao

import androidx.room.*
import fr.iutvannes.dual.model.persistence.Resultat

/**
 * Interface DAO pour l'entité Resultat.
 * Elle définit les méthodes d'accès aux données pour l'entité Resultat.
 *
 * @see Resultat
 */
@Dao
interface ResultatDAO {

    /**
     * Insère un nouveau résultat dans la base de données.
     *
     * @param resultat Le résultat à insérer
     */
    @Insert
    suspend fun insert(resultat: Resultat): Long

    /**
     * Supprime un résultat de la base de données.
     *
     * @param idResultat L'identifiant du résultat à supprimer
     */
    @Query("DELETE FROM Resultat WHERE id_resultat = :idResultat")
    suspend fun delete(idResultat: Int): Int

    /**
     * Récupère tous les résultats de la base de données.
     *
     * @return Une liste de tous les résultats
     */
    @Query("SELECT * FROM Resultat")
    suspend fun getAllResultats(): List<Resultat>

    /**
     * Récupère un résultat par son identifiant.
     *
     * @param idResultat L'identifiant du résultat à récupérer
     * @return Le résultat correspondant à l'identifiant, ou null si aucun résultat n'est trouvé
     */
    @Query("SELECT * FROM Resultat WHERE id_resultat = :idResultat")
    suspend fun getResultatById(idResultat: Int): Resultat?

    /**
     * Met à jour un résultat existant dans la base de données.
     *
     * @param resultat Le résultat à mettre à jour
     */
    @Update
    suspend fun update(resultat: Resultat): Int

    /**
     * Compte le nombre de résultats dans la base de données.
     *
     * @return Le nombre de résultats
     */
    @Query("SELECT COUNT(*) FROM Resultat")
    fun getCount(): Int
}