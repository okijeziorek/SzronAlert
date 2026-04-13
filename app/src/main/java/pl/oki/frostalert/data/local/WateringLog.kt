package pl.oki.frostalert.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "watering_log",
    indices = [Index(value = ["plantId"])]
)
data class WateringLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Dao
interface WateringLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WateringLog)

    @Delete
    suspend fun delete(log: WateringLog)

    @Query("SELECT * FROM watering_log WHERE plantId = :plantId ORDER BY timestamp DESC LIMIT 1")
    fun getLastWatering(plantId: Int): Flow<WateringLog?>

    @Query("SELECT * FROM watering_log ORDER BY timestamp DESC")
    fun getAllWaterings(): Flow<List<WateringLog>>

    @Query("""
        SELECT wl.plantId, MAX(wl.timestamp) as lastTimestamp
        FROM watering_log wl
        GROUP BY wl.plantId
    """)
    fun getLastWateringPerPlant(): Flow<List<LastWateringEntry>>

    @Query("DELETE FROM watering_log WHERE plantId = :plantId")
    suspend fun deleteForPlant(plantId: Int)
}

data class LastWateringEntry(
    val plantId: Int,
    val lastTimestamp: Long
)
