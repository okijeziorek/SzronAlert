package pl.oki.frostalert.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY category, name")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE category = :category ORDER BY name")
    fun getPlantsByCategory(category: String): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchPlants(query: String): Flow<List<Plant>>

    @Query("SELECT DISTINCT category FROM plants ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<Plant>)

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun getPlantCount(): Int
}

@Dao
interface UserPlantDao {
    @Query("""
        SELECT p.* FROM plants p 
        INNER JOIN user_plants up ON p.id = up.plantId 
        ORDER BY up.addedTimestamp DESC
    """)
    fun getUserPlants(): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUserPlant(userPlant: UserPlant)

    @Query("DELETE FROM user_plants WHERE plantId = :plantId")
    suspend fun removeUserPlant(plantId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM user_plants WHERE plantId = :plantId)")
    fun isPlantInGarden(plantId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM user_plants")
    fun getUserPlantCount(): Flow<Int>

    @Query("""
        SELECT MIN(p.frostThresholdCelsius) FROM plants p
        INNER JOIN user_plants up ON p.id = up.plantId
    """)
    fun getMostSensitiveFrostThreshold(): Flow<Double?>
}
