package pl.oki.frostalert.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import pl.oki.frostalert.receiver.GeofenceBroadcastReceiver
import java.util.Locale

private const val TAG = "GeofenceManager"

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val geofencePendingIntent: PendingIntent by lazy { createGeofencePendingIntent() }

    companion object {
        internal fun transitionTypes(loiteringDelayMs: Int): Int {
            var transitions = Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            if (loiteringDelayMs > 0) {
                transitions = transitions or Geofence.GEOFENCE_TRANSITION_DWELL
            }
            return transitions
        }
    }

    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = "pl.oki.frostalert.ACTION_GEOFENCE"
        }
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun registerGeofence(id: String, lat: Double, lon: Double, radiusMeters: Float = 20000f, loiteringDelayMs: Int = 60_000) {
        // Verify permission first
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot register geofence id=$id: ACCESS_FINE_LOCATION permission missing")
            return
        }

        val requestId = if (id.startsWith("geofence:")) id else "geofence:${"%.6f".format(Locale.US, lat)}:${"%.6f".format(Locale.US, lon)}"

        val builder = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(lat, lon, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes(loiteringDelayMs))

        // If caller provided a non-negative loitering delay, include DWELL transition and set the delay
        if (loiteringDelayMs > 0) {
            builder.setLoiteringDelay(loiteringDelayMs)
        }

        val geofence = try {
            builder.build()
        } catch (_: IllegalArgumentException) {
            Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(lat, lon, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener { Log.i(TAG, "Geofence registered: id=$requestId lat=$lat lon=$lon radius=$radiusMeters") }
                .addOnFailureListener { e ->
                    val code = if (e is ApiException) e.statusCode else -1
                    Log.e(TAG, "Failed to register geofence id=$requestId code=$code: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering geofence: ${e.message}")
        }
    }

    fun unregisterGeofence() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener { Log.i(TAG, "Geofence unregistered") }
                .addOnFailureListener { e -> Log.w(TAG, "Failed to unregister geofence: ${e.message}") }
        } catch (e: Exception) {
            Log.w(TAG, "Exception unregistering geofence: ${e.message}")
        }
    }
}
