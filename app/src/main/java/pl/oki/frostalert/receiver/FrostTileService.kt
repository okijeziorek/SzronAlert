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
                
                // Pobieramy ostatnią znaną lokalizację (uproszczenie dla Tile)
                // W wersji produkcyjnej można tu dodać pobieranie świeżego GPS
                val weather = OpenMeteoApi.getWeather(prefs.manualLatitude, prefs.manualLongitude)
                val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                
                val hasRisk = WeatherCalculations.hasFrostRisk(
                    minTemp, weather.current.humidity, weather.current.precipitation, 
                    weather.current.weatherCode, 
                    if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                    if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                    if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                    sensitivity = prefs.sensitivity
                )

                launch(Dispatchers.Main) {
                    tile.label = if (hasRisk) "Ryzyko: TAK" else "Ryzyko: NIE"
                    tile.state = if (hasRisk) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
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
