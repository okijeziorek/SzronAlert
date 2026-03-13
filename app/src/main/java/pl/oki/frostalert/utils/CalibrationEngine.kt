package pl.oki.frostalert.utils

import pl.oki.frostalert.data.local.CalibrationFeedback
import kotlin.math.abs

object CalibrationEngine {

    /**
     * Wyniki analizy kalibracji
     */
    data class CalibrationResult(
        val recommendedTempThreshold: Double,
        val recommendedHumidityThreshold: Int,
        val recommendedSensitivity: Double,
        val accuracyPercentage: Double,
        val totalFeedback: Int,
        val confidenceLevel: ConfidenceLevel
    )

    enum class ConfidenceLevel {
        LOW,      // < 10 feedbacków
        MEDIUM,   // 10-50 feedbacków
        HIGH      // > 50 feedbacków
    }

    /**
     * Przeprowadza analizę kalibracji na podstawie zebranych feedbacków
     */
    fun analyzeCalibration(feedbackList: List<CalibrationFeedback>): CalibrationResult {
        if (feedbackList.isEmpty()) {
            return CalibrationResult(
                recommendedTempThreshold = 1.0, // domyślne
                recommendedHumidityThreshold = 75,
                recommendedSensitivity = 1.0,
                accuracyPercentage = 0.0,
                totalFeedback = 0,
                confidenceLevel = ConfidenceLevel.LOW
            )
        }

        // Oblicz dokładność predykcji
        val accuratePredictions = feedbackList.count { it.actualFrostOccurred == it.predictedRisk }
        val accuracyPercentage = (accuratePredictions.toDouble() / feedbackList.size) * 100.0

        // Analizuj przypadki błędów
        val falsePositives = feedbackList.filter { it.predictedRisk && !it.actualFrostOccurred }
        val falseNegatives = feedbackList.filter { !it.predictedRisk && it.actualFrostOccurred }

        // Oblicz rekomendowane thresholdy
        val recommendedThresholds = calculateRecommendedThresholds(feedbackList, falsePositives, falseNegatives)

        val confidenceLevel = when {
            feedbackList.size < 10 -> ConfidenceLevel.LOW
            feedbackList.size < 50 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.HIGH
        }

        return CalibrationResult(
            recommendedTempThreshold = recommendedThresholds.tempThreshold,
            recommendedHumidityThreshold = recommendedThresholds.humidityThreshold,
            recommendedSensitivity = recommendedThresholds.sensitivity,
            accuracyPercentage = accuracyPercentage,
            totalFeedback = feedbackList.size,
            confidenceLevel = confidenceLevel
        )
    }

    /**
     * Oblicza rekomendowane thresholdy na podstawie błędów
     */
    private fun calculateRecommendedThresholds(
        allFeedback: List<CalibrationFeedback>,
        falsePositives: List<CalibrationFeedback>,
        falseNegatives: List<CalibrationFeedback>
    ): Thresholds {

        // Bazowe thresholdy (średnia z wszystkich feedbacków)
        val avgTempThreshold = allFeedback.map { it.usedThreshold }.average()
        val avgHumidityThreshold = allFeedback.map { it.usedHumidityThreshold }.average().toInt()
        val avgSensitivity = allFeedback.map { it.usedSensitivity }.average()

        var recommendedTempThreshold = avgTempThreshold
        var recommendedHumidityThreshold = avgHumidityThreshold
        var recommendedSensitivity = avgSensitivity

        // Dostosuj na podstawie błędów
        if (falsePositives.isNotEmpty()) {
            // Za dużo fałszywych alarmów - zwiększ thresholdy (zrób bardziej konserwatywny)
            val avgFalsePositiveTemp = falsePositives.map { it.temperature }.average()
            if (avgFalsePositiveTemp > recommendedTempThreshold) {
                recommendedTempThreshold = (recommendedTempThreshold + avgFalsePositiveTemp) / 2.0
            }
            recommendedSensitivity *= 1.1 // Zwiększ czułość (mniej alarmów)
        }

        if (falseNegatives.isNotEmpty()) {
            // Za mało alarmów - zmniejsz thresholdy (zrób bardziej czuły)
            val avgFalseNegativeTemp = falseNegatives.map { it.temperature }.average()
            if (avgFalseNegativeTemp < recommendedTempThreshold) {
                recommendedTempThreshold = (recommendedTempThreshold + avgFalseNegativeTemp) / 2.0
            }
            recommendedSensitivity *= 0.9 // Zmniejsz czułość (więcej alarmów)
        }

        // Ogranicz zakresy do rozsądnych wartości
        recommendedTempThreshold = recommendedTempThreshold.coerceIn(-5.0, 5.0)
        recommendedHumidityThreshold = recommendedHumidityThreshold.coerceIn(50, 90)
        recommendedSensitivity = recommendedSensitivity.coerceIn(0.5, 2.0)

        return Thresholds(
            tempThreshold = recommendedTempThreshold,
            humidityThreshold = recommendedHumidityThreshold,
            sensitivity = recommendedSensitivity
        )
    }

    /**
     * Sprawdza czy warto pokazać użytkownikowi prośbę o feedback
     */
    fun shouldAskForFeedback(lastFeedbackTimestamp: Long, currentTime: Long = System.currentTimeMillis()): Boolean {
        val hoursSinceLastFeedback = (currentTime - lastFeedbackTimestamp) / (1000 * 60 * 60)
        return hoursSinceLastFeedback >= 24 // Co najmniej raz dziennie
    }

    /**
     * Generuje wiadomość wyjaśniającą rekomendacje kalibracji
     */
    fun generateCalibrationExplanation(result: CalibrationResult): String {
        return when (result.confidenceLevel) {
            ConfidenceLevel.LOW -> "Zbierz więcej danych (min. 10 feedbacków) aby otrzymać dokładniejsze rekomendacje."
            ConfidenceLevel.MEDIUM -> "Na podstawie ${result.totalFeedback} feedbacków, dokładność predykcji wynosi ${"%.1f".format(result.accuracyPercentage)}%."
            ConfidenceLevel.HIGH -> "Na podstawie ${result.totalFeedback} feedbacków, algorytm osiąga ${"%.1f".format(result.accuracyPercentage)}% dokładności. Rekomendowane thresholdy zostały obliczone."
        }
    }

    /**
     * Sprawdza czy rekomendacje znacząco różnią się od aktualnych ustawień
     */
    fun shouldSuggestCalibration(
        currentTempThreshold: Double,
        currentHumidityThreshold: Int,
        currentSensitivity: Double,
        recommended: CalibrationResult
    ): Boolean {
        val tempDiff = abs(currentTempThreshold - recommended.recommendedTempThreshold)
        val humidityDiff = abs(currentHumidityThreshold - recommended.recommendedHumidityThreshold)
        val sensitivityDiff = abs(currentSensitivity - recommended.recommendedSensitivity)

        // Sugeruj jeśli różnice są znaczące
        return tempDiff >= 0.5 || humidityDiff >= 5 || sensitivityDiff >= 0.2
    }

    private data class Thresholds(
        val tempThreshold: Double,
        val humidityThreshold: Int,
        val sensitivity: Double
    )
}
