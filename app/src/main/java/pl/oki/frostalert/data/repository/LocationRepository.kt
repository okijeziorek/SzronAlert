package pl.oki.frostalert.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.oki.frostalert.data.local.SettingsDataStore
import kotlin.coroutines.resume

class LocationRepository(private val context: Context, private val settingsDataStore: SettingsDataStore) {

    suspend fun getEffectiveLocation(): Location? {
        val prefs = settingsDataStore.userPreferencesFlow.first()
        
        return if (prefs.isManualLocationEnabled) {
            Location("manual").apply {
                latitude = prefs.manualLatitude
                longitude = prefs.manualLongitude
            }
        } else {
            getCurrentGpsLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentGpsLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // Jeśli ostatnia lokalizacja jest świeża (np. młodsza niż 20 min), używamy jej
                if (location != null && (System.currentTimeMillis() - location.time) < 20 * 60 * 1000) {
                    continuation.resume(location)
                } else {
                    // W przeciwnym razie żądamy nowej, ale z balansem energii
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { freshLocation ->
                        continuation.resume(freshLocation)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }
}
