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
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.oki.frostalert.geofence.GeofenceRegistrarContract
import pl.oki.frostalert.utils.AnalyticsHelper
import pl.oki.frostalert.worker.FrostCheckWorker
import pl.oki.frostalert.worker.IntegrityCheckWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FrostApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var geofenceRegistrar: GeofenceRegistrarContract

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
        initFirebase()
        // Initialize AdMob off the main thread to reduce cold-start ANR risk.
        // The SDK routes its completion callback back to main via Handler internally.
        applicationScope.launch { MobileAds.initialize(this@FrostApplication) }
        setupRecurringWork()
        // Start reactive geofence observer so geofences are kept in sync with
        // user settings and location changes from app startup onwards.
        geofenceRegistrar.start()
    }

    /**
     * Initializes Firebase Analytics and Crashlytics using credentials from [BuildConfig].
     * The credentials are read from `local.properties` at build time and are never committed
     * to VCS. If credentials are absent (empty strings), Firebase is skipped gracefully.
     */
    private fun initFirebase() {
        val appId = BuildConfig.FIREBASE_APP_ID
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        if (appId.isEmpty() || projectId.isEmpty() || apiKey.isEmpty()) {
            Log.d("FrostApplication", "Firebase credentials not configured - Analytics/Crashlytics disabled")
            return
        }
        runCatching {
            val options = FirebaseOptions.Builder()
                .setApplicationId(appId)
                .setProjectId(projectId)
                .setApiKey(apiKey)
                .build()
            FirebaseApp.initializeApp(this, options)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            AnalyticsHelper.init(this)
            Log.d("FrostApplication", "Firebase initialized (project: $projectId)")
        }.onFailure {
            Log.w("FrostApplication", "Firebase initialization failed: ${it.message}")
        }
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. Frost check worker (hourly)
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

        // 2. Security integrity check worker (every 24 hours)
        val integrityCheckRequest = PeriodicWorkRequestBuilder<IntegrityCheckWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // flex interval - can run within 15 min window
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            IntegrityCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            integrityCheckRequest
        )
    }
}
