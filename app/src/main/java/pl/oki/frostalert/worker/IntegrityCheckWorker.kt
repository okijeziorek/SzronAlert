package pl.oki.frostalert.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.oki.frostalert.billing.BillingClientWrapper
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.security.HookDetector
import pl.oki.frostalert.security.IntegrityChecker
import pl.oki.frostalert.security.RootDetector
import pl.oki.frostalert.security.SecurityManager
import pl.oki.frostalert.utils.AppTelemetry
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.R

/**
 * Periodic worker that checks app integrity in the background.
 * Runs every 24 hours to detect tampering, root, hooking frameworks.
 *
 * If security issues are detected:
 * - Logs security event to telemetry
 * - Optionally shows warning notification
 * - Invalidates PRO status if tampering detected
 */
@HiltWorker
class IntegrityCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val securityManager: SecurityManager,
    private val settingsDataStore: SettingsDataStore,
    private val billingClientWrapper: BillingClientWrapper
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "IntegrityCheckWorker"
        const val WORK_NAME = "security_integrity_check"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic integrity check")

        return try {
            // Perform comprehensive security check
            val securityResult = securityManager.performSecurityCheck()

            // Log security status
            Log.d(TAG, "Security check completed: isSecure=${securityResult.isSecure}, " +
                    "integrity=${securityResult.integrity.verdict}, " +
                    "root=${securityResult.rootDetected}, " +
                    "tampering=${securityResult.tamperingDetected}")

            // Check individual security aspects
            val integrityFailed = !securityResult.integrity.isUnmodified ||
                                  !securityResult.integrity.isGenuine
            val signatureFailed = !IntegrityChecker.verifyAppSignature(appContext)
            val installerFailed = !IntegrityChecker.verifyInstaller(appContext)

            // Handle security failures
            if (integrityFailed || securityResult.rootDetected || securityResult.tamperingDetected) {
                handleSecurityFailure(securityResult)
            } else if (signatureFailed || installerFailed) {
                handleSignatureFailure(signatureFailed, installerFailed)
            } else {
                Log.i(TAG, "Integrity check passed - no security issues detected")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed with exception", e)
            AppTelemetry.recordSecurityEvent(
                appContext,
                AppTelemetry.SecurityEvent(
                    type = "integrity_check_error",
                    severity = "medium",
                    details = "Worker exception: ${e.message}"
                )
            )
            Result.failure()
        }
    }

    private suspend fun handleSecurityFailure(securityResult: SecurityManager.SecurityCheckResult) {
        val issues = mutableListOf<String>()

        if (!securityResult.integrity.isUnmodified) {
            issues.add("App modified")
        }
        if (!securityResult.integrity.isGenuine) {
            issues.add("Non-genuine device")
        }
        if (securityResult.rootDetected) {
            issues.add("Root detected")
        }
        if (securityResult.tamperingDetected) {
            issues.addAll(securityResult.tamperingIssues)
        }

        val issuesSummary = issues.joinToString(", ")

        Log.w(TAG, "Security issues detected: $issuesSummary")

        // Record security event with all detected issues
        AppTelemetry.recordSecurityEvent(
            appContext,
            AppTelemetry.SecurityEvent(
                type = "periodic_integrity_failed",
                severity = "critical",
                details = "Issues: $issuesSummary | Verdict: ${securityResult.integrity.verdict}"
            )
        )

        // For critical tampering (modified app), invalidate PRO status
        if (!securityResult.integrity.isUnmodified || securityResult.tamperingDetected) {
            withContext(Dispatchers.Main) {
                // Force disable PRO if app was modified or hooked
                settingsDataStore.updateIsProForced(false)
                Log.w(TAG, "PRO status invalidated due to tampering")
            }

            // Show security warning notification
            showSecurityWarning(issuesSummary)
        }

        // For root detection only (less severe), just log and warn
        if (securityResult.rootDetected && securityResult.integrity.isUnmodified) {
            Log.i(TAG, "Root detected but app not modified - logging only")
            AppTelemetry.recordSecurityEvent(
                appContext,
                AppTelemetry.SecurityEvent(
                    type = "root_detected",
                    severity = "high",
                    details = "Periodic check found root"
                )
            )
        }
    }

    private suspend fun handleSignatureFailure(signatureFailed: Boolean, installerFailed: Boolean) {
        val issues = mutableListOf<String>()

        if (signatureFailed) {
            issues.add("Invalid signature")
        }
        if (installerFailed) {
            issues.add("Non-Play Store installer")
        }

        Log.w(TAG, "Signature/installer issues: ${issues.joinToString(", ")}")

        AppTelemetry.recordSecurityEvent(
            appContext,
            AppTelemetry.SecurityEvent(
                type = "signature_mismatch",
                severity = if (signatureFailed) "critical" else "medium",
                details = issues.joinToString(", ")
            )
        )

        // Only invalidate PRO for actual signature tampering
        if (signatureFailed) {
            settingsDataStore.updateIsProForced(false)
            showSecurityWarning("Wykryto modyfikację aplikacji")
        }
    }

    private suspend fun showSecurityWarning(issues: String) {
        try {
            NotificationHelper.createNotificationChannel(appContext)
            // Use the suspend version of sendNotification
            NotificationHelper.sendNotification(
                appContext,
                title = appContext.getString(R.string.security_warning_title),
                message = appContext.getString(R.string.security_warning_message, issues)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show security notification", e)
        }
    }
}
