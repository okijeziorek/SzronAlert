package pl.oki.frostalert.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val frostThresholdCelsius: Double,
    val description: String = "",
    val iconEmoji: String = "🌱"
)

@Entity(tableName = "user_plants")
data class UserPlant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val addedTimestamp: Long = System.currentTimeMillis()
)
