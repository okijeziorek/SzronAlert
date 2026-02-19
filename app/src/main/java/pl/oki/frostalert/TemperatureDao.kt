package pl.oki.frostalert

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.oki.frostalert.data.TemperatureRecord

@Dao
interface TemperatureDao {
    @Insert
    suspend fun insert(record: TemperatureRecord)

    @Query("SELECT * FROM temperature_records ORDER BY timestamp DESC LIMIT 30")
    fun getRecentRecords(): Flow<List<TemperatureRecord>>

    @Query("SELECT COUNT(*) FROM temperature_records WHERE hasRisk = 1")
    suspend fun getRiskCount(): Int

    @Query("SELECT AVG(minTemp) FROM temperature_records")
    suspend fun getAverageMinTemp(): Double?
}