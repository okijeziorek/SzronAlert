package pl.oki.frostalert.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.AppError

object OpenMeteoApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getWeather(latitude: Double, longitude: Double): AppResult<WeatherResponse> {
        return try {
            // Dodano pola letnie: uv_index (aktualny i max) oraz daily_precipitation_sum
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,uv_index" +
                    "&hourly=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,uv_index" +
                    "&daily=uv_index_max,precipitation_sum&timezone=auto"
            
            val response = client.get(url).body<WeatherResponse>()
            AppResult.Success(response)
        } catch (e: Exception) {
            AppResult.Error(AppError.fromThrowable(e))
        }
    }
}

@Serializable
data class WeatherResponse(
    val current: CurrentWeather,
    val hourly: HourlyForecast,
    val daily: DailyForecast? = null
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Double,
    val precipitation: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("uv_index") val uvIndex: Double = 0.0
)

@Serializable
data class HourlyForecast(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("relative_humidity_2m") val humidity: List<Double>,
    val precipitation: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double>,
    @SerialName("uv_index") val uvIndex: List<Double> = emptyList()
)

@Serializable
data class DailyForecast(
    val time: List<String>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>
)
