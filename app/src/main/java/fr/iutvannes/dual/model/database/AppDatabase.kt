package fr.iutvannes.dual.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.model.persistence.Prof
import fr.iutvannes.dual.model.persistence.Resultat
import fr.iutvannes.dual.model.persistence.Seance
import fr.iutvannes.dual.model.dao.EleveDAO
import fr.iutvannes.dual.model.dao.ProfDAO
import fr.iutvannes.dual.model.dao.ResultatDAO
import fr.iutvannes.dual.model.dao.SeanceDAO

@Database(
    entities = [Eleve::class, Prof::class, Resultat::class, Seance::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun EleveDao(): EleveDAO
    abstract fun profDAO() : ProfDAO
    abstract fun resultatDao(): ResultatDAO
    abstract fun seanceDao(): SeanceDAO
}
