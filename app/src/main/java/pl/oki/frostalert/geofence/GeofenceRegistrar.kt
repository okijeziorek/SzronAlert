package pl.oki.frostalert.geofence

import android.content.Context
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

    private val scopeJob: Job = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)
    private val stateMutex = Mutex()

    // last registered state to avoid unnecessary registrations
    private var lastRegistered: RegisteredGeofence? = null
    private var running = false

    override fun registerForCurrentLocation(locationRepo: LocationRepository) {
        // maintain backward compatibility: perform an immediate one-shot registration
        scope.launch {
            val prefs = settingsDataStore.userPreferencesFlow.first()
            if (!prefs.isGeofencingEnabled) return@launch
            val loc = locationRepo.getEffectiveLocation()
            if (loc != null) {
                val radius = prefs.geofenceRadiusMeters.toFloat()
                val id = makeId(loc.latitude, loc.longitude)
                geofenceManager.registerGeofence(id, loc.latitude, loc.longitude, radius)
                stateMutex.withLock { lastRegistered = RegisteredGeofence(loc.latitude, loc.longitude, radius) }
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

        // Combine user preferences and a location-emitting flow created by polling getEffectiveLocation
        // We'll create a simple flow by mapping prefs -> location on each prefs emission.
        scope.launch {
            settingsDataStore.userPreferencesFlow
                .debounce(debounceMs)
                .onEach { prefs ->
                    try {
                        val loc = locationRepository.getEffectiveLocation()
                        syncWith(prefs.isGeofencingEnabled, loc, prefs.geofenceRadiusMeters.toFloat())
                    } catch (_: Throwable) {
                        // log and continue
                    }
                }
                .collect()
        }
    }

    override fun stop() {
        if (!running) return
        running = false
        // cancel ongoing coroutine scope job to stop collectors
        scopeJob.cancel()
        // optionally unregister geofence
        scope.launch {
            geofenceManager.unregisterGeofence()
            stateMutex.withLock { lastRegistered = null }
        }
    }

    override fun isRunning(): Boolean = running

    private suspend fun syncWith(enabled: Boolean, location: android.location.Location?, radius: Float) {
        stateMutex.withLock {
            if (!enabled || location == null) {
                if (lastRegistered != null) {
                    geofenceManager.unregisterGeofence()
                    lastRegistered = null
                }
                return
            }

            val desired = RegisteredGeofence(location.latitude, location.longitude, radius)
            if (!isSame(desired, lastRegistered)) {
                // register new
                try {
                    val id = makeId(desired.lat, desired.lon)
                    geofenceManager.registerGeofence(id, desired.lat, desired.lon, desired.radius)
                    lastRegistered = desired
                } catch (t: Throwable) {
                    // log but don't crash; will retry on next emission
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

    private fun makeId(lat: Double, lon: Double) = "geofence:${"%.6f".format(lat)}:${"%.6f".format(lon)}"

    private data class RegisteredGeofence(val lat: Double, val lon: Double, val radius: Float)
}


