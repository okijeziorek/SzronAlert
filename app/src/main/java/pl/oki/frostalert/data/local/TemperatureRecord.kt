package pl.oki.frostalert.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "temperature_records",
    indices = [Index(value = ["timestamp"])]
)
data class TemperatureRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val minTemp: Double,
    val hasRisk: Boolean,
    val frostProbability: Int = 0
)
