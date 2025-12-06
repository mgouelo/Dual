package fr.iutvannes.dual.model.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Classe")
data class Classe(
    @PrimaryKey(autoGenerate = true)
    var id_classe: Int = 0,
    //variable nom unique
    var nom: String = ""
)