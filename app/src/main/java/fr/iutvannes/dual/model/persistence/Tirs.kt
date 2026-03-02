package fr.iutvannes.dual.model.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Tirs")
data class Tirs(
    @PrimaryKey(autoGenerate = true)
    var id_tirs: Int = 0,
    var id_seance: Int = 0,
    var id_eleve: Int = 0,
    var nb_tirs_reussi: List<Int> = emptyList(),
    var temps_au_pas_de_tir: List<Int> = emptyList()
)