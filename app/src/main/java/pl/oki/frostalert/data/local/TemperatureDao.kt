package pl.oki.frostalert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TemperatureDao {
    @Insert
    suspend fun insert(record: TemperatureRecord)

    @Query("SELECT * FROM temperature_records ORDER BY timestamp DESC LIMIT 30")
    fun getRecentRecords(): Flow<List<TemperatureRecord>>

    @Query("SELECT * FROM temperature_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<TemperatureRecord>

    @Query("SELECT COUNT(*) FROM temperature_records WHERE hasRisk = 1")
    suspend fun getRiskCount(): Int

    @Query("SELECT AVG(minTemp) FROM temperature_records")
    suspend fun getAverageMinTemp(): Double?

    // Nowe zapytania dla Fazy 3: Ekspert Sezonowy
    
    @Query("SELECT COUNT(*) FROM temperature_records WHERE hasRisk = 1 AND timestamp >= :since")
    suspend fun getRiskCountSince(since: Long): Int

    @Query("SELECT MIN(minTemp) FROM temperature_records")
    suspend fun getAbsoluteMinTemp(): Double?

    @Query("SELECT AVG(minTemp) FROM temperature_records WHERE timestamp >= :since")
    suspend fun getAverageMinTempSince(since: Long): Double?

    @Query("SELECT * FROM temperature_records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getRecordsBetween(start: Long, end: Long): List<TemperatureRecord>
}
