package fr.iutvannes.dual.model.persistence
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "Prof",
    indices = [Index(value = ["email"], unique = true)]
)
data class Prof(
    @PrimaryKey(autoGenerate = true)
    var id_prof: Int = 0,
    var nom: String = "",
    var prenom: String = "",
    var email: String = "",
    var password: String = "",
    val photoUri: String? = null
)