package fr.iutvannes.dual.model.persistence.converters

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromList(value: List<Int>): String =
        value.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<Int> =
        if (value.isEmpty()) emptyList()
        else value.split(",").map { it.toInt() }
}