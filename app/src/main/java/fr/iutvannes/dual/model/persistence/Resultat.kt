package fr.iutvannes.dual.model.persistence

import androidx.room.*

@Entity(
    tableName = "Resultat",
    /** A refaire plus tard au moment de la création des séances
    foreignKeys = [
        ForeignKey(
            entity = Eleve::class,
            parentColumns = ["id_eleve"],
            childColumns = ["id_eleve"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Seance::class,
            parentColumns = ["id_seance"],
            childColumns = ["id_seance"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_eleve"), Index("id_seance")]
    */
)
data class Resultat (
    @PrimaryKey(autoGenerate = true)
    var id_resultat: Int=0,
    var id_eleve : Int = 0,
    var id_seance: Int = 0,
    var temp_course : Float = 0F,
    var cibles_touchees: Int = 0,
    var penalites: Float = 0F,
    var vma: Float = 0F,
    var note_finale: Float = 0F,
    var classement: Int = 0
)