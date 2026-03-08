package pl.oki.frostalert.receiver

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.R

class FrostTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Sprawdzanie..."
        tile.updateTile()

        serviceScope.launch {
            try {
                val settingsDataStore = SettingsDataStore(applicationContext)
                val prefs = settingsDataStore.userPreferencesFlow.first()
                
                val weatherResult = OpenMeteoApi.getWeather(prefs.manualLatitude, prefs.manualLongitude)
                
                launch(Dispatchers.Main) {
                    when (weatherResult) {
                        is AppResult.Success -> {
                            val weather = weatherResult.data
                            val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                            
                            val hasRisk = WeatherCalculations.hasFrostRisk(
                                temp = minTemp, 
                                humidity = weather.current.humidity, 
                                precip = weather.current.precipitation, 
                                weatherCode = weather.current.weatherCode, 
                                tempThreshold = if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                                humidityThreshold = if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                                precipitationThreshold = if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                                sensitivity = prefs.sensitivity,
                                windSpeed = weather.current.windSpeed
                            )

                            tile.label = if (hasRisk) "Ryzyko: TAK" else "Ryzyko: NIE"
                            tile.state = if (hasRisk) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        }
                        is AppResult.Error -> {
                            tile.label = "Błąd sieci"
                            tile.state = Tile.STATE_UNAVAILABLE
                        }
                    }
                    tile.updateTile()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    tile.label = "Błąd"
                    tile.state = Tile.STATE_UNAVAILABLE
                    tile.updateTile()
                }
            }
        }
    }

    private fun updateTile() {
        val tile = qsTile
        tile.label = "FrostAlert"
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
