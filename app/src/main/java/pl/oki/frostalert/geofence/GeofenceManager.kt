package pl.oki.frostalert.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import pl.oki.frostalert.receiver.GeofenceBroadcastReceiver

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun registerGeofence(id: String, lat: Double, lon: Double, radiusMeters: Float = 20000f, loiteringDelayMs: Int = 60_000) {
        // Verify permission first
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // permission missing, cannot register
            return
        }

        val builder = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(lat, lon, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

        // If caller provided a non-negative loitering delay, include DWELL transition and set the delay
        if (loiteringDelayMs > 0) {
            builder.setLoiteringDelay(loiteringDelayMs)
            builder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
        } else {
            // Otherwise only monitor ENTER
            builder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        }

        val geofence = builder.build()

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                .addOnSuccessListener { /* zarejestrowano */ }
                .addOnFailureListener { /* loguj */ }
        } catch (e: SecurityException) {
            // Brak permisji - loguj
        }
    }

    fun unregisterGeofence() {
        try {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener { /* usunięto */ }
                .addOnFailureListener { /* loguj */ }
        } catch (e: Exception) {
            // log
        }
    }
}

