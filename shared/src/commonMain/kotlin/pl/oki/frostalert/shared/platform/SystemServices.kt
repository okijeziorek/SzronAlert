package pl.oki.frostalert.shared.platform

import pl.oki.frostalert.shared.network.GeoPoint

interface LocationService {
    suspend fun currentLocation(): GeoPoint?
}

interface NotificationService {
    suspend fun scheduleFrostAlert(title: String, body: String)
    suspend fun cancelAllAlerts()
}

interface BackgroundTaskScheduler {
    suspend fun scheduleHourlyRefresh()
    suspend fun scheduleDailyIntegrityCheck()
}

interface LocalStorage {
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String): String?
}

interface SubscriptionService {
    suspend fun availableProducts(): List<SubscriptionProduct>
    suspend fun restorePurchases(): Boolean
}

interface SecurityService {
    suspend fun runRuntimeChecks(): SecurityCheckResult
}

data class SubscriptionProduct(
    val id: String,
    val priceDisplay: String,
    val trialAvailable: Boolean
)

data class SecurityCheckResult(
    val isEnvironmentTrusted: Boolean,
    val issues: List<String> = emptyList()
)
