package pl.oki.frostalert.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "geofence_record",
    indices = [Index(value = ["timestamp"])]
)
data class GeofenceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val direction: String?,
    val minTemp: Double,
    val hasRisk: Boolean,
    val riskLevel: Double,
    val locationName: String?
)
