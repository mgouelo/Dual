package fr.iutvannes.dual.model.persistence

import androidx.room.*

/**
 * Class representing a result
 *
 * @param id_resultat unique identifier of the result
 * @param id_eleve student associated with the result
 * @param id_seance session associated with the result
 * @param temp_course race time
 * @param cibles_touchees number of targets hit
 * @param penalites penalties
 * @param vma vma
 * @param note_finale final grade
 * @param classement ranking
 */
@Entity(
    tableName = "Resultat",
    /** To be done again later when creating the sessions
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