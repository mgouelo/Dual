package fr.iutvannes.dual.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import fr.iutvannes.dual.model.persistence.Classe

/**
 * Interface DAO pour l'entité Classe.
 * Elle définit les méthodes d'accès aux données pour l'entité Classe.
 *
 * @see Classe
 */
@Dao
interface ClasseDAO {


    /**
     * Insère une nouvelle classe dans la base de données.
     *
     * @param classe La classe à insérer.
     */
    @Insert
    suspend fun insert(classe: Classe): Long

    /**
     * Supprime une classe existante dans la base de données.
     *
     * @param idClasse L'identifiant de la classe à supprimer.
     */
    @Query("DELETE FROM Classe WHERE id_classe = :idClasse")
    suspend fun delete(idClasse: Int): Int

    /**
     * Récupère une classe par son nom.
     *
     * @param nomClasse Le nom de la classe à rechercher.
     * @return La classe trouvée, ou null si aucune classe n'est trouvée.
     */
    @Query("SELECT * FROM Classe WHERE nom = :nomClasse")
    suspend fun getClasseByName(nomClasse: String): Classe?

    /**
     * Récupère tous les noms de classes existantes dans la base de données.
     *
     * @return Une liste de chaînes de caractères contenant les noms de classes.
     */
    @Query("SELECT nom FROM Classe")
    suspend fun getClasses(): List<String>

    /**
     * Récupère toutes les classes existantes dans la base de données.
     *
     * @return Une liste de classes contenant toutes les classes existantes
     */
    @Query("SELECT * FROM Classe")
    fun getAllClasses(): List<Classe>
}