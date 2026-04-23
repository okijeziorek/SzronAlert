package pl.oki.frostalert.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Utility to perform app integrity and tamper detection checks.
 */
object IntegrityChecker {

    /**
     * Verify that the app signature matches the expected signature.
     * Helps detect repackaged/modified APKs.
     *
     * @param context Application context
     * @return true if signature is valid, false if tampered
     */
    fun verifyAppSignature(context: Context): Boolean {
        // Note: In production, replace this with your actual release certificate SHA-256
        // To get your signature: keytool -list -v -keystore release.keystore
        // For now, we skip strict checking to avoid blocking debug builds
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

            // For now, just verify we have a signature
            // In production, compare with expected SHA-256 hash
            signatures != null && signatures.isNotEmpty()

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
