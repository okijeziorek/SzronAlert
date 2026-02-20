package pl.oki.frostalert.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object OpenMeteoApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherResponse {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,precipitation,weather_code&hourly=temperature_2m,relative_humidity_2m,precipitation,weather_code"
        return client.get(url).body()
    }
}

@Serializable
data class WeatherResponse(
    val current: CurrentWeather,
    val hourly: HourlyForecast
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m")
    val temperature: Double,
    @SerialName("relative_humidity_2m")
    val humidity: Double,
    val precipitation: Double,
    @SerialName("weather_code")
    val weatherCode: Int
)

@Serializable
data class HourlyForecast(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temperature: List<Double>,
    @SerialName("relative_humidity_2m")
    val humidity: List<Double>,
    val precipitation: List<Double>,
    @SerialName("weather_code")
    val weatherCode: List<Int>
)
