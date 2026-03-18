package pl.oki.frostalert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Insert
    suspend fun insert(record: GeofenceRecord)

    @Query("SELECT * FROM geofence_record ORDER BY timestamp DESC LIMIT 100")
    fun getRecentGeofenceRecords(): Flow<List<GeofenceRecord>>

    @Query("DELETE FROM geofence_record WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}

