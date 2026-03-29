package pl.oki.frostalert

import android.app.Application
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FrostApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    // Application-scoped coroutine scope for fire-and-forget background tasks.
    // SupervisorJob ensures a child failure doesn't cancel the whole scope.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // WorkManager reads this lazily via Configuration.Provider — do NOT call
    // WorkManager.initialize() manually; that would trigger a double-init crash.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob off the main thread to reduce cold-start ANR risk.
        // The SDK routes its completion callback back to main via Handler internally.
        applicationScope.launch { MobileAds.initialize(this@FrostApplication) }
        setupRecurringWork()
        // Start reactive geofence observer so geofences are kept in sync with
        // user settings and location changes from app startup onwards.
        geofenceRegistrar.start()
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<FrostCheckWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            // Exponential backoff: first retry after 15 min, capped by WorkManager at ~5 h
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        // KEEP: do not interrupt an already-running or enqueued instance.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "frost_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}
