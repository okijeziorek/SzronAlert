package pl.oki.frostalert.data.remote

data class Weather(
    val current: CurrentWeather,
    val hourly: HourlyForecast
)
