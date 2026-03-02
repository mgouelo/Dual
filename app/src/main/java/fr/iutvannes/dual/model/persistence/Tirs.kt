package fr.iutvannes.dual.model.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Tirs",
    foreignKeys = [
        ForeignKey(
            entity = Seance::class,
            parentColumns = ["id_seance"],
            childColumns = ["id_seance"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Eleve::class,
            parentColumns = ["id_eleve"],
            childColumns = ["id_eleve"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_seance"), Index("id_eleve")]
)
data class Tirs(
    @PrimaryKey(autoGenerate = true)
    var id_tirs: Int = 0,
    var id_seance: Int = 0,
    var id_eleve: Int = 0,
    var nb_tirs_reussi: List<Int> = emptyList(),
    var temps_au_pas_de_tir: List<Int> = emptyList()
)