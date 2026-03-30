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
    var id_resultat: Int = 0,
    var id_eleve: Int = 0,
    var id_seance: Int = 0,
    var temp_course: Float = 0F,
    var cibles_touchees: Int = 0,
    var penalites: Float = 0F,
    var vma: Float = 0F,
    var note_finale: Float = 0F,
    var classement: Int = 0,
    var nbTours: Int = 0,
    var ecart_max_course: Int = 0,
    // Détail épreuve 4ème
    var temps_A: Int = 0,       // arrivée tir 1 (secondes)
    var temps_B: Int = 0,       // sortie tir 1
    var temps_C: Int = 0,       // arrivée tir 2
    var temps_D: Int = 0,       // sortie tir 2
    var temps_E: Int = 0,       // arrivée finale
    var tir1: Int = 0,          // réussites série 1
    var tir2: Int = 0,          // réussites série 2
    var note_intensite: Float = 0F,
    var note_efficience: Float = 0F,
    var note_vma: Float = 0F,
    var ressenti_intensite: String = "",
    var ressenti_durer: String = "",
    var ressenti_lucidite: String = ""
)