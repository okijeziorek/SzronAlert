package pl.oki.frostalert.shared.network

import pl.oki.frostalert.shared.model.WeatherResponseDto

data class GeoPoint(val latitude: Double, val longitude: Double)

interface WeatherClient {
    suspend fun fetchWeather(point: GeoPoint): WeatherResponseDto
}

object OpenMeteoRequestBuilder {
    fun forecastUrl(point: GeoPoint): String {
        return "https://api.open-meteo.com/v1/forecast?latitude=${point.latitude}&longitude=${point.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,uv_index" +
            "&hourly=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,uv_index" +
            "&daily=temperature_2m_min,temperature_2m_max,weather_code,uv_index_max,precipitation_sum" +
            "&forecast_days=16&timezone=auto"
    }
}
