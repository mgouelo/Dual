package fr.iutvannes.dual.model.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Courses")
data class Courses(
    @PrimaryKey(autoGenerate = true)
    var id_courses: Int = 0,
    var id_seance: Int = 0,
    var id_eleve: Int = 0,
    var vitesse: Double = 0.0,
    var temps_au_tour: List<Int> = emptyList()
)