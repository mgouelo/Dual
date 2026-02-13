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
 * An abstract class that represents the application's database.
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
     * Abstract method that returns an object of type [ClasseDAO].
     *
     * @return [ClasseDAO]
     */
    abstract fun EleveDao(): EleveDAO

    /**
     * Abstract method that returns an object of type [ProfDAO].
     *
     * @return [ProfDAO]
     */
    abstract fun profDAO() : ProfDAO

    /**
     * Abstract method that returns an object of type [ResultatDAO].
     *
     * @return [ResultatDAO]
     */
    abstract fun resultatDao(): ResultatDAO

    /**
     * Abstract method that returns an object of type [SeanceDAO].
     *
     * @return [SeanceDAO]
     */
    abstract fun seanceDao(): SeanceDAO

    /**
     * Abstract method that returns an object of type [ClasseDAO].
     *
     * @return [ClasseDAO]
     */
    abstract fun classeDao(): ClasseDAO
}
