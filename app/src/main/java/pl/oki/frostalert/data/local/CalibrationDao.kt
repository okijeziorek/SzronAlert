package pl.oki.frostalert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibrationDao {

    @Insert
    suspend fun insertFeedback(feedback: CalibrationFeedback)

    @Query("SELECT * FROM calibration_feedback ORDER BY timestamp DESC LIMIT 100")
    fun getRecentFeedback(): Flow<List<CalibrationFeedback>>

    @Query("SELECT COUNT(*) FROM calibration_feedback WHERE actualFrostOccurred = predictedRisk")
    suspend fun getAccuratePredictionsCount(): Int

    @Query("SELECT COUNT(*) FROM calibration_feedback")
    suspend fun getTotalFeedbackCount(): Int

    @Query("SELECT AVG(usedThreshold) FROM calibration_feedback WHERE actualFrostOccurred = 1")
    suspend fun getAverageThresholdForFrost(): Double?

    @Query("SELECT AVG(usedThreshold) FROM calibration_feedback WHERE actualFrostOccurred = 0")
    suspend fun getAverageThresholdForNoFrost(): Double?

    @Query("SELECT * FROM calibration_feedback WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getFeedbackSince(sinceTimestamp: Long): List<CalibrationFeedback>

    @Query("DELETE FROM calibration_feedback WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteOldFeedback(olderThanTimestamp: Long)

    @Query("DELETE FROM calibration_feedback")
    suspend fun clearAllFeedback()
}
