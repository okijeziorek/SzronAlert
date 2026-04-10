package pl.oki.frostalert.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY isDefault DESC, name ASC")
    fun getAllLocations(): Flow<List<SavedLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: SavedLocation): Long

    @Delete
    suspend fun delete(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE saved_locations SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE saved_locations SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Int)

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getById(id: Int): SavedLocation?

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun getCount(): Int
}
