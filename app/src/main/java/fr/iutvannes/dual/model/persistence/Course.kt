package fr.iutvannes.dual.model.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

//table parent
@Entity(tableName = "Course")
data class Course(
    @PrimaryKey(autoGenerate = true)
    var id_course: Int = 0,
    var id_seance: Int = 0,
    var id_eleve: Int = 0,
    var distance_tour: Double = 0.0,
    var note_course: Int? = null
)

// table enfant (Les informations de chaque tour)
@Entity(
    tableName = "TourCourse",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id_course"],
            childColumns = ["id_course"],
            onDelete = ForeignKey.CASCADE // si on supprime la Course, tous ses tours sont supprimés
        )
    ],
    indices = [Index("id_course")]
)
data class TourCourse(
    @PrimaryKey(autoGenerate = true)
    var id_tour: Int = 0,
    var id_course: Int = 0,
    var numero_tour: Int = 0, // 1 pour le 1er tour, 2 pour le 2e etc
    var temps_ms: Long = 0L // milliseconde
)

// Modèle de vue pour Room (Ce n'est pas une table en BDD)
// permet de récupérer facilement la Course avec tous ses tours dans le DAO
data class CourseAvecTours(
    @Embedded
    var course: Course,

    @Relation(
        parentColumn = "id_course",
        entityColumn = "id_course"
    )
    var liste_tours: List<TourCourse> = emptyList()
)