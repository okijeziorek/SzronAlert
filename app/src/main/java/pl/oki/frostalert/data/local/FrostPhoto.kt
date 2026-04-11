package pl.oki.frostalert.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "frost_photos")
data class FrostPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureRecordId: Int? = null,
    val note: String = ""
)

@Dao
interface FrostPhotoDao {
    @Query("SELECT * FROM frost_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<FrostPhoto>>

    @Query("SELECT * FROM frost_photos WHERE temperatureRecordId = :recordId")
    fun getPhotosForRecord(recordId: Int): Flow<List<FrostPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: FrostPhoto): Long

    @Delete
    suspend fun delete(photo: FrostPhoto)

    @Query("SELECT COUNT(*) FROM frost_photos")
    suspend fun getCount(): Int
}
