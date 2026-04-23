package pl.oki.frostalert.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.security.IntegrityChecker
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
 * - Shows warning notification for all detected threats
 * - Invalidates PRO status only if tampering or signature modification is detected
 */
@HiltWorker
class IntegrityCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val securityManager: SecurityManager,
    private val settingsDataStore: SettingsDataStore
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
            val signatureFailed = !IntegrityChecker.verifyAppSignature(appContext)
            val installerFailed = !IntegrityChecker.verifyInstaller(appContext)

            // Handle security failures
            if (!securityResult.integrity.isUnmodified || securityResult.tamperingDetected ||
                signatureFailed || installerFailed) {
                handleTamperingFailure(securityResult, signatureFailed, installerFailed)
            } else if (securityResult.rootDetected) {
                handleRootOnlyDetection()
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

    /**
     * Handles cases where the app appears modified, hooked, or has an invalid signature.
     * Invalidates PRO status and shows a warning notification.
     */
    private suspend fun handleTamperingFailure(
        securityResult: SecurityManager.SecurityCheckResult,
        signatureFailed: Boolean,
        installerFailed: Boolean
    ) {
        val issues = mutableListOf<String>()

        if (!securityResult.integrity.isUnmodified) {
            issues.add(appContext.getString(R.string.security_issue_app_modified))
        }
        if (!securityResult.integrity.isGenuine) {
            issues.add(appContext.getString(R.string.security_issue_non_genuine))
        }
        if (securityResult.rootDetected) {
            issues.add(appContext.getString(R.string.security_issue_root_detected))
        }
        if (securityResult.tamperingDetected) {
            issues.addAll(securityResult.tamperingIssues)
        }
        if (signatureFailed) {
            issues.add(appContext.getString(R.string.security_issue_invalid_signature))
        }
        if (installerFailed) {
            issues.add(appContext.getString(R.string.security_issue_non_play_installer))
        }

        val issuesSummary = issues.joinToString(", ")
        Log.w(TAG, "Security issues detected: $issuesSummary")

        // Record security event
        AppTelemetry.recordSecurityEvent(
            appContext,
            AppTelemetry.SecurityEvent(
                type = "periodic_integrity_failed",
                severity = "critical",
                details = "Issues: $issuesSummary; Verdict: ${securityResult.integrity.verdict}"
            )
        )

        // Invalidate PRO status — DataStore suspends on its own IO dispatcher; no Main switch needed
        settingsDataStore.updateIsProForced(false)
        Log.w(TAG, "PRO status invalidated due to tampering")

        // Notify the user
        showSecurityWarning(issuesSummary)
    }

    /**
     * Handles the case where root is detected but the app itself appears unmodified.
     * Logs the event and warns the user without invalidating PRO status.
     */
    private suspend fun handleRootOnlyDetection() {
        Log.i(TAG, "Root detected but app not modified - warning user")

        AppTelemetry.recordSecurityEvent(
            appContext,
            AppTelemetry.SecurityEvent(
                type = "root_detected",
                severity = "high",
                details = "Periodic check found root on unmodified app"
            )
        )

        showRootWarning()
    }

    private suspend fun showSecurityWarning(issues: String) {
        try {
            NotificationHelper.createNotificationChannel(appContext)
            NotificationHelper.sendNotification(
                appContext,
                title = appContext.getString(R.string.security_warning_title),
                message = appContext.getString(R.string.security_warning_message, issues)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show security notification", e)
        }
    }

    private suspend fun showRootWarning() {
        try {
            NotificationHelper.createNotificationChannel(appContext)
            NotificationHelper.sendNotification(
                appContext,
                title = appContext.getString(R.string.security_warning_title),
                message = appContext.getString(R.string.security_warning_root_message)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show root warning notification", e)
        }
    }
}

