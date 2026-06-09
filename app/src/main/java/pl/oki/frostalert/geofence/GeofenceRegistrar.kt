package pl.oki.frostalert.geofence

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.repository.LocationRepository
import kotlin.math.abs
import java.util.Locale

/**
 * Reaktywna implementacja rejestratora geofence.
 * Nasłuchuje `SettingsDataStore.userPreferencesFlow` oraz przyrostowo pobiera
 * lokalizację z `LocationRepository.getEffectiveLocation()` (wywoływaną jako Flow poprzez
 * combine z userPreferencesFlow) i synchronizuje geofences za pomocą GeofenceManager.
 */
@OptIn(FlowPreview::class)
class GeofenceRegistrar(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val locationRepository: LocationRepository,
    private val geofenceManager: GeofenceManager = GeofenceManager(context),
    private val debounceMs: Long = 2_000L
) : GeofenceRegistrarContract {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()
    private var observeJob: Job? = null

    // last registered state to avoid unnecessary registrations
    private var lastRegistered: RegisteredGeofence? = null
    private var running = false

    companion object {
        private const val TAG = "GeofenceRegistrar"
    }

    override fun registerForCurrentLocation(locationRepo: LocationRepository) {
        scope.launch {
            try {
                val prefs = settingsDataStore.userPreferencesFlow.first()
                if (!prefs.isGeofencingEnabled) return@launch
                val loc = locationRepo.getEffectiveLocation() ?: run {
                    Log.w(TAG, "registerForCurrentLocation: location unavailable")
                    return@launch
                }
                syncWith(enabled = true, location = loc, radius = prefs.geofenceRadiusMeters.toFloat())
            } catch (e: Throwable) {
                Log.w(TAG, "registerForCurrentLocation failed: ${e.message}")
            }
        }
    }

    override fun unregister() {
        scope.launch {
            geofenceManager.unregisterGeofence()
            stateMutex.withLock { lastRegistered = null }
        }
    }

    override fun start() {
        if (running) return
        running = true
        Log.i(TAG, "start: beginning geofence observation")

        observeJob = scope.launch {
            settingsDataStore.userPreferencesFlow
                .debounce(debounceMs)
                .onEach { prefs ->
                    try {
                        val loc = locationRepository.getEffectiveLocation()
                        syncWith(prefs.isGeofencingEnabled, loc, prefs.geofenceRadiusMeters.toFloat())
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error syncing geofence on preferences change: ${e.message}")
                    }
                }
                .collect()
        }
    }

    override fun stop() {
        if (!running) return
        running = false
        Log.i(TAG, "stop: stopping geofence observation")
        observeJob?.cancel()
        observeJob = null
        scope.launch {
            try {
                geofenceManager.unregisterGeofence()
            } finally {
                stateMutex.withLock { lastRegistered = null }
            }
        }
    }

    override fun isRunning(): Boolean = running

    private suspend fun syncWith(enabled: Boolean, location: android.location.Location?, radius: Float) {
        stateMutex.withLock {
            if (!enabled || location == null) {
                if (lastRegistered != null) {
                    Log.i(TAG, "syncWith: geofencing disabled or no location — unregistering")
                    geofenceManager.unregisterGeofence()
                    lastRegistered = null
                }
                return
            }

            val desired = RegisteredGeofence(location.latitude, location.longitude, radius)
            if (!isSame(desired, lastRegistered)) {
                try {
                    val id = makeId(desired.lat, desired.lon)
                    Log.i(TAG, "syncWith: registering new geofence id=$id radius=$radius")
                    geofenceManager.registerGeofence(id, desired.lat, desired.lon, desired.radius)
                    lastRegistered = desired
                } catch (e: Throwable) {
                    Log.e(TAG, "syncWith: registration failed, will retry on next emission: ${e.message}")
                    // will retry on next flow emission
                }
            }
        }
    }

    private fun isSame(a: RegisteredGeofence?, b: RegisteredGeofence?): Boolean {
        if (a == null || b == null) return false
        val latClose = abs(a.lat - b.lat) < 1e-5
        val lonClose = abs(a.lon - b.lon) < 1e-5
        val radiusClose = abs(a.radius - b.radius) < 0.5f
        return latClose && lonClose && radiusClose
    }

    internal fun makeId(lat: Double, lon: Double) =
        "geofence:${"%.6f".format(Locale.US, lat)}:${"%.6f".format(Locale.US, lon)}"

    private data class RegisteredGeofence(val lat: Double, val lon: Double, val radius: Float)
}

