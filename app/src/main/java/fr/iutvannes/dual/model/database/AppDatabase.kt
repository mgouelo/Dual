package fr.iutvannes.dual.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.iutvannes.dual.model.dao.ClasseDAO
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.model.persistence.Prof
import fr.iutvannes.dual.model.persistence.Resultat
import fr.iutvannes.dual.model.persistence.Seance
import fr.iutvannes.dual.model.dao.EleveDAO
import fr.iutvannes.dual.model.dao.ProfDAO
import fr.iutvannes.dual.model.dao.ResultatDAO
import fr.iutvannes.dual.model.dao.SeanceDAO
import fr.iutvannes.dual.model.persistence.Classe

/**
 * Classe abstraite qui représente la base de données de l'application.
 *
 * @see ClasseDAO
 * @see EleveDAO
 * @see ProfDAO
 * @see ResultatDAO
 * @see SeanceDAO
 * @see Classe
 * @see Eleve
 * @see Prof
 * @see Resultat
 * @see Seance
 */
@Database(
    entities = [Eleve::class, Prof::class, Resultat::class, Seance::class, Classe::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Méthode abstraite qui retourne un objet de type [ClasseDAO].
     *
     * @return [ClasseDAO]
     */
    abstract fun EleveDao(): EleveDAO

    /**
     * Méthode abstraite qui retourne un objet de type [ProfDAO].
     *
     * @return [ProfDAO]
     */
    abstract fun profDAO() : ProfDAO

    /**
     * Méthode abstraite qui retourne un objet de type [ResultatDAO].
     *
     * @return [ResultatDAO]
     */
    abstract fun resultatDao(): ResultatDAO

    /**
     * Méthode abstraite qui retourne un objet de type [SeanceDAO].
     *
     * @return [SeanceDAO]
     */
    abstract fun seanceDao(): SeanceDAO

    /**
     * Méthode abstraite qui retourne un objet de type [ClasseDAO].
     *
     * @return [ClasseDAO]
     */
    abstract fun classeDao(): ClasseDAO
}
