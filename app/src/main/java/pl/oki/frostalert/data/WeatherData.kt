package pl.oki.frostalert.data

data class Weather(
    val current: CurrentWeather,
    val hourly: HourlyForecast
)
