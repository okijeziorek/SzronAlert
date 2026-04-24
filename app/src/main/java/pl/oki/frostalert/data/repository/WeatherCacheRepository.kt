package pl.oki.frostalert.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.oki.frostalert.data.remote.WeatherResponse
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

@Singleton
class WeatherCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private val WEATHER_DATA_KEY = stringPreferencesKey("weather_data")
        private val CACHE_TIMESTAMP_KEY = longPreferencesKey("cache_timestamp")
    }

    suspend fun saveWeatherData(weather: WeatherResponse) {
        context.weatherCacheDataStore.edit { preferences ->
            val jsonString = json.encodeToString(weather)
            preferences[WEATHER_DATA_KEY] = jsonString
            preferences[CACHE_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun getCachedWeatherData(): WeatherResponse? {
        val preferences = context.weatherCacheDataStore.data.first()
        val jsonString = preferences[WEATHER_DATA_KEY] ?: return null
        val timestamp = preferences[CACHE_TIMESTAMP_KEY] ?: return null

        // Check if cache is still valid
        if (System.currentTimeMillis() - timestamp > CACHE_DURATION_MS) {
            clearCache()
            return null
        }

        return try {
            json.decodeFromString<WeatherResponse>(jsonString)
        } catch (e: Exception) {
            clearCache()
            null
        }
    }

    /**
     * Returns cached weather data together with its timestamp, regardless of whether
     * the cache has expired. Useful for offline mode — shows the last known state even
     * when the TTL has elapsed.
     *
     * @return Pair of (WeatherResponse, cacheTimestampMs) or null if no cache exists.
     */
    suspend fun getAnyCachedWeather(): Pair<WeatherResponse, Long>? {
        val preferences = context.weatherCacheDataStore.data.first()
        val jsonString = preferences[WEATHER_DATA_KEY] ?: return null
        val timestamp = preferences[CACHE_TIMESTAMP_KEY] ?: return null
        return try {
            json.decodeFromString<WeatherResponse>(jsonString) to timestamp
        } catch (e: Exception) {
            clearCache()
            null
        }
    }

    suspend fun clearCache() {
        context.weatherCacheDataStore.edit { preferences ->
            preferences.remove(WEATHER_DATA_KEY)
            preferences.remove(CACHE_TIMESTAMP_KEY)
        }
    }

    fun isCacheValid(): Flow<Boolean> {
        return context.weatherCacheDataStore.data.map { preferences ->
            val timestamp = preferences[CACHE_TIMESTAMP_KEY] ?: return@map false
            System.currentTimeMillis() - timestamp <= CACHE_DURATION_MS
        }
    }
}
