package fr.iutvannes.dual.model.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Class representing a class
 *
 * @param id_classe unique identifier of the class
 * @param nom class name
 */
@Entity(tableName = "Classe")
data class Classe(
    @PrimaryKey(autoGenerate = true)
    var id_classe: Int = 0,
    // variable unique name
    var nom: String = ""
)