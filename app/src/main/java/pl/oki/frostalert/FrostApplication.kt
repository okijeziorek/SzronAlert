package pl.oki.frostalert

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.concurrent.TimeUnit

class FrostApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        setupRecurringWork()
    }

    private fun setupRecurringWork() {
        // Zmieniono na CONNECTED, aby alerty działały też na danych mobilnych (nie tylko WiFi)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<FrostCheckWorker>(
            1, TimeUnit.HOURS // Zmniejszono interwał do 1h dla większej precyzji nocnej
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "frost_check_work",
            ExistingPeriodicWorkPolicy.UPDATE, // Używamy UPDATE, aby nowe parametry weszły w życie
            repeatingRequest
        )
    }
}
