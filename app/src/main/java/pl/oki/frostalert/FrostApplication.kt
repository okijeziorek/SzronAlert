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
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<FrostCheckWorker>(
            3, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "frost_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}
