package fr.iutvannes.dual.model.persistence

import androidx.room.PrimaryKey
import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
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
    var vma: Float? = null,
    var couleur_parcours: String? = null // coupelles jaunes / plots verts / coupelles bleues / plots bleus / coupelles rouge / plots rouges / grand tour
    )