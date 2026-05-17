package pl.oki.frostalert.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.oki.frostalert.data.local.CalibrationFeedback

class CalibrationEngineTest {

    // ── shouldAskForFeedback ──────────────────────────────────────────────────

    @Test
    fun `shouldAskForFeedback returns false when no predicted risk`() {
        val shouldAsk = CalibrationEngine.shouldAskForFeedback(
            lastFeedbackTimestamp = 0L,
            predictedRisk = false,
            minHoursBetweenFeedback = 12,
            currentTime = 48L * 60 * 60 * 1000
        )

        assertFalse(shouldAsk)
    }

    @Test
    fun `shouldAskForFeedback returns false when risk exists but interval is too short`() {
        val shouldAsk = CalibrationEngine.shouldAskForFeedback(
            lastFeedbackTimestamp = 10L * 60 * 60 * 1000,
            predictedRisk = true,
            minHoursBetweenFeedback = 12,
            currentTime = 20L * 60 * 60 * 1000
        )

        assertFalse(shouldAsk)
    }

    @Test
    fun `shouldAskForFeedback returns true when risk exists and interval is met`() {
        val shouldAsk = CalibrationEngine.shouldAskForFeedback(
            lastFeedbackTimestamp = 10L * 60 * 60 * 1000,
            predictedRisk = true,
            minHoursBetweenFeedback = 12,
            currentTime = 22L * 60 * 60 * 1000
        )

        assertTrue(shouldAsk)
    }

    // ── analyzeCalibration ────────────────────────────────────────────────────

    @Test
    fun `analyzeCalibration returns default values for empty list`() {
        val result = CalibrationEngine.analyzeCalibration(emptyList())

        assertEquals(1.0, result.recommendedTempThreshold, 0.001)
        assertEquals(75, result.recommendedHumidityThreshold)
        assertEquals(1.0, result.recommendedSensitivity, 0.001)
        assertEquals(0.0, result.accuracyPercentage, 0.001)
        assertEquals(0, result.totalFeedback)
        assertEquals(CalibrationEngine.ConfidenceLevel.LOW, result.confidenceLevel)
    }

    @Test
    fun `analyzeCalibration calculates 100 percent accuracy when all predictions correct`() {
        val feedbackList = listOf(
            makeFeedback(predictedRisk = true, actualFrostOccurred = true),
            makeFeedback(predictedRisk = false, actualFrostOccurred = false),
            makeFeedback(predictedRisk = true, actualFrostOccurred = true)
        )

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        assertEquals(100.0, result.accuracyPercentage, 0.001)
        assertEquals(3, result.totalFeedback)
    }

    @Test
    fun `analyzeCalibration calculates partial accuracy`() {
        val feedbackList = listOf(
            makeFeedback(predictedRisk = true, actualFrostOccurred = true),   // correct
            makeFeedback(predictedRisk = true, actualFrostOccurred = false),  // false positive
            makeFeedback(predictedRisk = false, actualFrostOccurred = false), // correct
            makeFeedback(predictedRisk = false, actualFrostOccurred = true)   // false negative
        )

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        assertEquals(50.0, result.accuracyPercentage, 0.001)
        assertEquals(4, result.totalFeedback)
    }

    @Test
    fun `analyzeCalibration adjusts sensitivity up for false positives`() {
        // False positives: predicted risk but no frost occurred
        val feedbackList = (1..5).map {
            makeFeedback(predictedRisk = true, actualFrostOccurred = false, temperature = 5.0, threshold = 1.0)
        }

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        // Sensitivity should be increased (> 1.0) when there are too many false positives
        assertTrue("Sensitivity should increase for false positives", result.recommendedSensitivity > 1.0)
    }

    @Test
    fun `analyzeCalibration adjusts sensitivity down for false negatives`() {
        // False negatives: no predicted risk but frost occurred
        val feedbackList = (1..5).map {
            makeFeedback(predictedRisk = false, actualFrostOccurred = true, temperature = -1.0, threshold = 1.0)
        }

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        // Sensitivity should be decreased (< 1.0) when there are too many false negatives
        assertTrue("Sensitivity should decrease for false negatives", result.recommendedSensitivity < 1.0)
    }

    @Test
    fun `analyzeCalibration returns LOW confidence for fewer than 10 feedbacks`() {
        val feedbackList = (1..9).map { makeFeedback(predictedRisk = true, actualFrostOccurred = true) }

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        assertEquals(CalibrationEngine.ConfidenceLevel.LOW, result.confidenceLevel)
    }

    @Test
    fun `analyzeCalibration returns MEDIUM confidence for 10 to 49 feedbacks`() {
        val feedbackList = (1..25).map { makeFeedback(predictedRisk = true, actualFrostOccurred = true) }

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        assertEquals(CalibrationEngine.ConfidenceLevel.MEDIUM, result.confidenceLevel)
    }

    @Test
    fun `analyzeCalibration returns HIGH confidence for 50 or more feedbacks`() {
        val feedbackList = (1..50).map { makeFeedback(predictedRisk = true, actualFrostOccurred = true) }

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        assertEquals(CalibrationEngine.ConfidenceLevel.HIGH, result.confidenceLevel)
    }

    @Test
    fun `analyzeCalibration clamps thresholds within valid range`() {
        // Use extreme values to test clamping
        val feedbackList = listOf(
            makeFeedback(predictedRisk = true, actualFrostOccurred = false, temperature = 100.0, threshold = 50.0)
        )

        val result = CalibrationEngine.analyzeCalibration(feedbackList)

        assertTrue("Temp threshold should be clamped to [-5, 5]",
            result.recommendedTempThreshold in -5.0..5.0)
        assertTrue("Humidity threshold should be clamped to [50, 90]",
            result.recommendedHumidityThreshold in 50..90)
        assertTrue("Sensitivity should be clamped to [0.5, 2.0]",
            result.recommendedSensitivity in 0.5..2.0)
    }

    // ── shouldSuggestCalibration ──────────────────────────────────────────────

    @Test
    fun `shouldSuggestCalibration returns true when temp difference is 0_5 or more`() {
        val recommended = CalibrationEngine.analyzeCalibration(
            listOf(makeFeedback(predictedRisk = true, actualFrostOccurred = true))
        ).copy(recommendedTempThreshold = 1.5)

        assertTrue(
            CalibrationEngine.shouldSuggestCalibration(
                currentTempThreshold = 1.0,
                currentHumidityThreshold = 75,
                currentSensitivity = 1.0,
                recommended = recommended
            )
        )
    }

    @Test
    fun `shouldSuggestCalibration returns true when humidity difference is 5 or more`() {
        val recommended = CalibrationEngine.analyzeCalibration(
            listOf(makeFeedback(predictedRisk = true, actualFrostOccurred = true))
        ).copy(recommendedHumidityThreshold = 80)

        assertTrue(
            CalibrationEngine.shouldSuggestCalibration(
                currentTempThreshold = 1.0,
                currentHumidityThreshold = 75,
                currentSensitivity = 1.0,
                recommended = recommended
            )
        )
    }

    @Test
    fun `shouldSuggestCalibration returns true when sensitivity difference is 0_2 or more`() {
        val recommended = CalibrationEngine.analyzeCalibration(
            listOf(makeFeedback(predictedRisk = true, actualFrostOccurred = true))
        ).copy(recommendedSensitivity = 1.25) // clearly >= 0.2 above current value of 1.0

        assertTrue(
            CalibrationEngine.shouldSuggestCalibration(
                currentTempThreshold = 1.0,
                currentHumidityThreshold = 75,
                currentSensitivity = 1.0,
                recommended = recommended
            )
        )
    }

    @Test
    fun `shouldSuggestCalibration returns false when all differences are small`() {
        val recommended = CalibrationEngine.analyzeCalibration(
            listOf(makeFeedback(predictedRisk = true, actualFrostOccurred = true))
        ).copy(
            recommendedTempThreshold = 1.0,
            recommendedHumidityThreshold = 75,
            recommendedSensitivity = 1.0
        )

        assertFalse(
            CalibrationEngine.shouldSuggestCalibration(
                currentTempThreshold = 1.0,
                currentHumidityThreshold = 75,
                currentSensitivity = 1.0,
                recommended = recommended
            )
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeFeedback(
        predictedRisk: Boolean,
        actualFrostOccurred: Boolean,
        temperature: Double = 0.0,
        threshold: Double = 1.0
    ) = CalibrationFeedback(
        timestamp = System.currentTimeMillis(),
        actualFrostOccurred = actualFrostOccurred,
        predictedRisk = predictedRisk,
        temperature = temperature,
        humidity = 80,
        weatherCode = 0,
        locationLat = 52.0,
        locationLon = 21.0,
        appMode = 0,
        usedThreshold = threshold,
        usedHumidityThreshold = 75,
        usedSensitivity = 1.0
    )
}
