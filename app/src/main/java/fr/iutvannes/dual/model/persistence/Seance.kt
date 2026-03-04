package fr.iutvannes.dual.model.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Seance",
    foreignKeys = [
        ForeignKey(
            entity = Prof::class,
            parentColumns = ["id_prof"],
            childColumns = ["id_prof"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_prof")]
)
data class Seance(
    @PrimaryKey(autoGenerate = true)
    var id_seance: Int = 0,
    var date: String = "",
    var nb_tours: Int = 0,
    var nb_cibles: Int = 0,
    var id_prof: Int = 0,
    var type: String,
    var classe: String
)