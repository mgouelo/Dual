package fr.iutvannes.dual.model.persistence

import androidx.room.PrimaryKey
import androidx.room.Entity
import androidx.room.Index

/**
 * Class representing a student
 *
 * @param id_eleve unique student identifier
 * @param nom student's name
 * @param prenom student's first name
 * @param genre student gender (M/F)
 * @param classe student's class
 */
@Entity(
    indices = [Index(value = ["nom", "prenom", "classe"], unique = true)]
)
data class Eleve(
    @PrimaryKey(autoGenerate = true)
    var id_eleve: Int = 0,
    var nom: String = "",
    var prenom: String = "",
    var genre : String = "",
    var classe: String = "",
    )