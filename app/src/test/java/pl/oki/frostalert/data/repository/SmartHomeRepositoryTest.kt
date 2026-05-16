package pl.oki.frostalert.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.utils.AppResult

class SmartHomeRepositoryTest {

    private val repository = SmartHomeRepository()

    // ── sendWebhook URL validation ────────────────────────────────────────────

    @Test
    fun `sendWebhook returns error for blank URL`() = runBlocking {
        val result = repository.sendWebhook(
            webhookUrl = "",
            frostProbability = 80,
            minTemp = -2.0,
            locationName = "Warszawa"
        )
        assertTrue("Blank URL should return error", result is AppResult.Error)
    }

    @Test
    fun `sendWebhook returns error for non-HTTPS URL`() = runBlocking {
        val result = repository.sendWebhook(
            webhookUrl = "http://example.com/webhook",
            frostProbability = 80,
            minTemp = -2.0,
            locationName = "Warszawa"
        )
        assertTrue("Non-HTTPS URL should return validation error", result is AppResult.Error)
    }

    @Test
    fun `sendWebhook returns error for whitespace-only URL`() = runBlocking {
        val result = repository.sendWebhook(
            webhookUrl = "   ",
            frostProbability = 80,
            minTemp = -2.0,
            locationName = "Warszawa"
        )
        assertTrue("Whitespace URL should return error", result is AppResult.Error)
    }

    @Test
    fun `sendWebhook returns error for malformed URL`() = runBlocking {
        val result = repository.sendWebhook(
            webhookUrl = "https://not a valid url with spaces",
            frostProbability = 80,
            minTemp = -2.0,
            locationName = "Warszawa"
        )
        assertTrue("Malformed URL should return validation error", result is AppResult.Error)
    }

    // ── sendIftttTrigger key validation ───────────────────────────────────────

    @Test
    fun `sendIftttTrigger returns error for blank key`() = runBlocking {
        val result = repository.sendIftttTrigger(
            iftttKey = "",
            frostProbability = 80,
            minTemp = -2.0,
            locationName = "Warszawa"
        )
        assertTrue("Blank IFTTT key should return error", result is AppResult.Error)
    }

    @Test
    fun `sendIftttTrigger returns error for whitespace-only key`() = runBlocking {
        val result = repository.sendIftttTrigger(
            iftttKey = "   ",
            frostProbability = 80,
            minTemp = -2.0,
            locationName = "Warszawa"
        )
        assertTrue("Whitespace-only IFTTT key should return error", result is AppResult.Error)
    }
}
