package fr.iutvannes.dual.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.iutvannes.dual.model.persistence.Classe
import fr.iutvannes.dual.model.persistence.Course
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.model.persistence.Prof
import fr.iutvannes.dual.model.persistence.Resultat
import fr.iutvannes.dual.model.persistence.Seance
import fr.iutvannes.dual.model.persistence.Tir
import fr.iutvannes.dual.model.dao.ClasseDAO
import fr.iutvannes.dual.model.dao.CourseDAO
import fr.iutvannes.dual.model.dao.EleveDAO
import fr.iutvannes.dual.model.dao.ProfDAO
import fr.iutvannes.dual.model.dao.ResultatDAO
import fr.iutvannes.dual.model.dao.SeanceDAO
import fr.iutvannes.dual.model.dao.TirDAO
import fr.iutvannes.dual.model.persistence.SalveTir
import fr.iutvannes.dual.model.persistence.TourCourse

@Database(
    entities = [Eleve::class, Prof::class, Resultat::class, Seance::class, Classe::class, Tir::class, Course::class, TourCourse::class, SalveTir::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun EleveDao(): EleveDAO
    abstract fun profDAO() : ProfDAO
    abstract fun resultatDao(): ResultatDAO
    abstract fun seanceDao(): SeanceDAO
    abstract fun classeDao(): ClasseDAO
    abstract fun tirDao(): TirDAO
    abstract fun courseDao(): CourseDAO
}
