package pl.oki.frostalert.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponseDto(
    val current: CurrentWeatherDto,
    val hourly: HourlyForecastDto,
    val daily: DailyForecastDto? = null
)

@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Double,
    val precipitation: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("uv_index") val uvIndex: Double = 0.0
)

@Serializable
data class HourlyForecastDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double?>,
    @SerialName("relative_humidity_2m") val humidity: List<Double?>,
    val precipitation: List<Double?>,
    @SerialName("weather_code") val weatherCode: List<Int?>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>,
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList()
)

@Serializable
data class DailyForecastDto(
    val time: List<String>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList()
)
