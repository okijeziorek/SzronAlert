package pl.oki.frostalert.security

import android.content.Context
import android.content.pm.PackageManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Utility to detect if device is rooted.
 * Implements multiple detection strategies to identify common root/jailbreak indicators.
 */
object RootDetector {

    /**
     * Performs comprehensive root detection check.
     * @return true if device appears to be rooted
     */
    fun isRooted(context: Context): Boolean {
        return checkRootFiles() ||
               checkSuperuserAPK(context) ||
               checkRWPaths() ||
               checkSuCommand()
    }

    /**
     * Check for common root binary files in system paths.
     */
    private fun checkRootFiles(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/system/app/SuperSU",
            "/system/app/SuperSU.apk"
        )

        return rootPaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Check for known root management apps.
     */
    private fun checkSuperuserAPK(context: Context): Boolean {
        val suApps = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk"
        )

        val pm = context.packageManager
        return suApps.any { packageName ->
            try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Check if system directories are writable (shouldn't be on non-rooted devices).
     */
    private fun checkRWPaths(): Boolean {
        val paths = arrayOf("/system", "/system/bin", "/system/sbin", "/system/xbin")

        return paths.any { path ->
            try {
                val file = File(path)
                file.exists() && file.canWrite()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Try to execute 'su' command to check if it's available.
     */
    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.destroy()
            !result.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Magisk Hide is active (basic detection).
     */
    fun isMagiskHideActive(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which su")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.destroy()
            !result.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
