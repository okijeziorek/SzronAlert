package pl.oki.frostalert.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import pl.oki.frostalert.BuildConfig
import java.security.MessageDigest

/**
 * Utility to perform app integrity and tamper detection checks.
 */
object IntegrityChecker {

    /**
     * Verify that the app signature matches the expected certificate SHA-256.
     *
     * The expected certificate hash is supplied via [BuildConfig.EXPECTED_SIGNING_CERT_SHA256].
     * When that field is empty (e.g. during development or when not yet configured),
     * strict matching is skipped and we only verify that *some* signature exists.
     * Set the field to the release certificate SHA-256 (hex string, colon-separated bytes)
     * before publishing to production:
     *   keytool -list -v -keystore release.keystore
     *
     * @param context Application context
     * @return true if signature is valid / matches expected, false if tampered or missing
     */
    fun verifyAppSignature(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            val expectedSha256 = BuildConfig.EXPECTED_SIGNING_CERT_SHA256
            if (expectedSha256.isBlank()) {
                // No expected value configured — only verify a signature is present
                return true
            }

            // Compare each signing certificate's SHA-256 against the expected value
            val digest = MessageDigest.getInstance("SHA-256")
            signatures.any { signature ->
                val certSha256 = digest.digest(signature.toByteArray())
                    .joinToString(":") { "%02X".format(it) }
                certSha256.equals(expectedSha256, ignoreCase = true)
            }

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify that the app was installed from a trusted source (Google Play Store).
     *
     * @param context Application context
     * @return true if installed from trusted source
     */
    fun verifyInstaller(context: Context): Boolean {
        val validInstallers = listOf(
            "com.android.vending",        // Google Play Store
            "com.google.android.feedback", // Google Play beta
            null  // Allow null for debug builds (adb install)
        )

        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }

            installer in validInstallers
        } catch (e: Exception) {
            // If we can't determine, assume it's okay (don't block users)
            true
        }
    }

    /**
     * Check if app is running in debug mode.
     * Release builds should not have debuggable flag set.
     *
     * @param context Application context
     * @return true if app is debuggable
     */
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
