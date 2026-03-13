package pl.oki.frostalert.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calibration_feedback")
data class CalibrationFeedback(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,                    // Kiedy feedback został zebrany
    val actualFrostOccurred: Boolean,       // Czy faktycznie wystąpił szron (true = tak, false = nie)
    val predictedRisk: Boolean,             // Czy app przewidziała ryzyko (true = tak, false = nie)
    val temperature: Double,                // Aktualna temperatura w momencie feedbacku
    val humidity: Int,                      // Wilgotność
    val weatherCode: Int,                   // Kod pogody
    val locationLat: Double,                // Szerokość geograficzna
    val locationLon: Double,                // Długość geograficzna
    val appMode: Int,                       // Tryb aplikacji (0=car, 1=garden)
    val usedThreshold: Double,              // Próg temperatury użyty w obliczeniach
    val usedHumidityThreshold: Int,         // Próg wilgotności użyty
    val usedSensitivity: Double             // Czułość użyta
)
