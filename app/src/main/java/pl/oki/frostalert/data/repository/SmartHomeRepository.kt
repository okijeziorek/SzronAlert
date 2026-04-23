package pl.oki.frostalert.data.repository

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.AppError
import javax.inject.Inject

class SmartHomeRepository @Inject constructor() {
    private val client = HttpClient()

    suspend fun sendWebhook(
        webhookUrl: String,
        frostProbability: Int,
        minTemp: Double,
        locationName: String
    ): AppResult<Boolean> {
        if (webhookUrl.isBlank()) return AppResult.Error(AppError.ValidationError("Webhook URL is empty"))

        // Security: Enforce HTTPS for webhooks
        if (!webhookUrl.startsWith("https://", ignoreCase = true)) {
            return AppResult.Error(AppError.ValidationError("Webhook URL must use HTTPS"))
        }

        // Validate URL format
        try {
            java.net.URL(webhookUrl).toURI()
        } catch (e: Exception) {
            return AppResult.Error(AppError.ValidationError("Invalid webhook URL format"))
        }

        return try {
            val body = buildJsonObject {
                put("frost_probability", frostProbability)
                put("min_temp", minTemp)
                put("location", locationName)
                put("source", "FrostAlert")
            }.toString()
            val response = client.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.isSuccess()) {
                AppResult.Success(true)
            } else {
                AppResult.Error(AppError.NetworkError("Webhook returned ${response.status}"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.fromThrowable(e))
        }
    }

    suspend fun sendIftttTrigger(
        iftttKey: String,
        eventName: String = "frost_alert",
        frostProbability: Int,
        minTemp: Double,
        locationName: String
    ): AppResult<Boolean> {
        if (iftttKey.isBlank()) return AppResult.Error(AppError.ValidationError("IFTTT key is empty"))
        val url = "https://maker.ifttt.com/trigger/$eventName/with/key/$iftttKey"
        return try {
            val body = buildJsonObject {
                put("value1", frostProbability.toString())
                put("value2", minTemp.toString())
                put("value3", locationName)
            }.toString()
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.isSuccess()) {
                AppResult.Success(true)
            } else {
                AppResult.Error(AppError.NetworkError("IFTTT returned ${response.status}"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.fromThrowable(e))
        }
    }
}
