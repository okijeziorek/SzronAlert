package pl.oki.frostalert.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app security checks including Play Integrity API verification.
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "SecurityManager"

    data class IntegrityResult(
        val isGenuine: Boolean,
        val isUnmodified: Boolean,
        val isLicensed: Boolean,
        val verdict: String
    )

    /**
     * Check app integrity using Google Play Integrity API.
     * This verifies the app hasn't been tampered with and is running on a genuine device.
     *
     * Note: Requires Google Cloud project number to be configured.
     * For basic security without cloud setup, falls back to client-side checks.
     */
    suspend fun checkAppIntegrity(): IntegrityResult {
        return try {
            // For now, use basic checks without Play Integrity API
            // To enable Play Integrity: Configure cloud project number and uncomment below
            performBasicIntegrityCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed", e)
            IntegrityResult(
                isGenuine = false,
                isUnmodified = false,
                isLicensed = false,
                verdict = "CHECK_FAILED: ${e.message}"
            )
        }
    }

    /**
     * Perform basic integrity checks without Play Integrity API.
     * Checks app signature, installer, and system properties.
     */
    private fun performBasicIntegrityCheck(): IntegrityResult {
        val signatureValid = IntegrityChecker.verifyAppSignature(context)
        val installerValid = IntegrityChecker.verifyInstaller(context)
        val notRooted = !RootDetector.isRooted(context)
        val notEmulator = !HookDetector.isEmulator()
        val notHooked = !HookDetector.isXposedActive() && !HookDetector.isFridaRunning()

        val isGenuine = notRooted && notEmulator && notHooked
        val isUnmodified = signatureValid && notHooked
        val isLicensed = installerValid

        val issues = mutableListOf<String>()
        if (!notRooted) issues.add("ROOTED")
        if (!notEmulator) issues.add("EMULATOR")
        if (!notHooked) issues.add("HOOKED")
        if (!signatureValid) issues.add("INVALID_SIGNATURE")
        if (!installerValid) issues.add("SIDELOADED")

        return IntegrityResult(
            isGenuine = isGenuine,
            isUnmodified = isUnmodified,
            isLicensed = isLicensed,
            verdict = if (issues.isEmpty()) "GENUINE" else issues.joinToString("|")
        )
    }

    /**
     * Advanced Play Integrity check using Google Cloud API.
     * Uncomment and configure when ready for production.
     */
    /*
    private suspend fun performPlayIntegrityCheck(): IntegrityResult {
        val integrityManager = IntegrityManagerFactory.create(context)

        val nonce = generateNonce()

        // TODO: Replace with your Google Cloud project number
        val cloudProjectNumber = 0L // Get from Google Cloud Console

        val request = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

        val response = integrityManager.requestIntegrityToken(request).await()
        val token = response.token()

        return parseIntegrityToken(token)
    }
    */

    private fun generateNonce(): String {
        return Base64.encodeToString(
            ByteArray(32).apply { SecureRandom().nextBytes(this) },
            Base64.URL_SAFE or Base64.NO_WRAP
        )
    }

    private fun parseIntegrityToken(token: String): IntegrityResult {
        try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return IntegrityResult(false, false, false, "INVALID_TOKEN")
            }

            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)

            val appIntegrity = json.optJSONObject("appIntegrity")?.optString("appRecognitionVerdict")
            val deviceIntegrity = json.optJSONObject("deviceIntegrity")?.optJSONArray("deviceRecognitionVerdict")
            val accountDetails = json.optJSONObject("accountDetails")?.optString("appLicensingVerdict")

            return IntegrityResult(
                isGenuine = deviceIntegrity?.toString()?.contains("MEETS_DEVICE_INTEGRITY") == true,
                isUnmodified = appIntegrity == "PLAY_RECOGNIZED",
                isLicensed = accountDetails == "LICENSED",
                verdict = "$appIntegrity | $deviceIntegrity | $accountDetails"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse integrity token", e)
            return IntegrityResult(false, false, false, "PARSE_FAILED")
        }
    }

    /**
     * Perform comprehensive security check.
     */
    suspend fun performSecurityCheck(): SecurityCheckResult {
        val integrity = checkAppIntegrity()
        val rootDetected = RootDetector.isRooted(context)
        val tamperingResult = HookDetector.detectTampering(context)

        return SecurityCheckResult(
            integrity = integrity,
            rootDetected = rootDetected,
            tamperingDetected = tamperingResult.isTampered,
            tamperingIssues = tamperingResult.issues,
            isSecure = integrity.isGenuine && integrity.isUnmodified && !rootDetected && !tamperingResult.isTampered
        )
    }

    data class SecurityCheckResult(
        val integrity: IntegrityResult,
        val rootDetected: Boolean,
        val tamperingDetected: Boolean,
        val tamperingIssues: List<String>,
        val isSecure: Boolean
    )
}
