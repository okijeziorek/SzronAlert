package pl.oki.frostalert.data.repository

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
        return try {
            val body = """{"frost_probability":$frostProbability,"min_temp":$minTemp,"location":"${locationName.replace("\"", "\\\"")}","source":"FrostAlert"}"""
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
            val body = """{"value1":"$frostProbability","value2":"$minTemp","value3":"${locationName.replace("\"", "\\\"")}"}"""
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
