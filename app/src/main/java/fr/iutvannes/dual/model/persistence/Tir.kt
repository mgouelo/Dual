package fr.iutvannes.dual.model.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "Tir")
data class Tir(
    @PrimaryKey(autoGenerate = true)
    var id_tir: Int = 0,
    var id_seance: Int = 0,
    var id_eleve: Int = 0,
    var note_tir: Int? = null,
    var note_regularite: Int? = null
)

// table enfant (Les informations de chaque passage au pas de tir)
@Entity(
    tableName = "SalveTir",
    foreignKeys = [
        ForeignKey(
            entity = Tir::class,
            parentColumns = ["id_tir"],
            childColumns = ["id_tir"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_tir")]
)
data class SalveTir(
    @PrimaryKey(autoGenerate = true)
    var id_passage: Int = 0,
    var id_tir: Int = 0,
    var numero_passage: Int = 0,
    var nb_tir_reussi: Int = 0,
    var temps_au_pas_de_tir_ms: Long = 0L
)

// récupérer facilement les données dans le DAO
data class TirAvecPassages(
    @Embedded
    var tir: Tir,

    @Relation(
        parentColumn = "id_tir",
        entityColumn = "id_tir"
    )
    var liste_passages: List<SalveTir> = emptyList()
)