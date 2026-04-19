package pl.oki.frostalert.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationEngineTest {

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
}
