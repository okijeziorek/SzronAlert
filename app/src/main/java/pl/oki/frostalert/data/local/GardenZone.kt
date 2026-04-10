package pl.oki.frostalert.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "garden_zones")
data class GardenZone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val temperatureCorrection: Double,
    val description: String = "",
    val iconEmoji: String = "🌡️",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface GardenZoneDao {
    @Query("SELECT * FROM garden_zones ORDER BY name ASC")
    fun getAllZones(): Flow<List<GardenZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zone: GardenZone): Long

    @Update
    suspend fun update(zone: GardenZone)

    @Delete
    suspend fun delete(zone: GardenZone)

    @Query("SELECT COUNT(*) FROM garden_zones")
    suspend fun getCount(): Int

    @Query("SELECT * FROM garden_zones WHERE id = :id")
    suspend fun getById(id: Int): GardenZone?
}
