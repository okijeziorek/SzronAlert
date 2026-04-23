package pl.oki.frostalert.security

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Utility to detect hooking frameworks (Xposed, Frida) and debugging tools.
 */
object HookDetector {

    /**
     * Check if Xposed Framework is active.
     */
    fun isXposedActive(): Boolean {
        return try {
            // Check stack trace for Xposed signatures
            throw Exception("StackCheck")
        } catch (e: Exception) {
            val stackTrace = e.stackTraceToString()
            stackTrace.contains("de.robv.android.xposed") ||
            stackTrace.contains("Xposed") ||
            stackTrace.contains("EdXposed") ||
            stackTrace.contains("LSPosed")
        }
    }

    /**
     * Check if Frida dynamic instrumentation framework is running.
     */
    fun isFridaRunning(): Boolean {
        // Check for Frida server on default ports
        return checkPort(27042) || checkPort(27043) || checkFridaLibraries()
    }

    /**
     * Check if a specific port is listening (indicates Frida server).
     */
    private fun checkPort(port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", port), 100)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for Frida-related libraries in process memory map.
     */
    private fun checkFridaLibraries(): Boolean {
        return try {
            val maps = File("/proc/self/maps")
            if (!maps.exists()) return false

            val content = maps.readText()
            content.contains("frida") ||
            content.contains("gum-js-loop") ||
            content.contains("libfrida")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device is an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
               Build.FINGERPRINT.startsWith("generic") ||
               Build.FINGERPRINT.startsWith("unknown") ||
               Build.HARDWARE.contains("goldfish") ||
               Build.HARDWARE.contains("ranchu") ||
               Build.MODEL.contains("google_sdk") ||
               Build.MODEL.contains("Emulator") ||
               Build.MODEL.contains("Android SDK") ||
               Build.MANUFACTURER.contains("Genymotion") ||
               Build.PRODUCT.contains("sdk_google") ||
               Build.PRODUCT.contains("google_sdk") ||
               Build.PRODUCT.contains("sdk") ||
               Build.PRODUCT.contains("sdk_x86") ||
               Build.PRODUCT.contains("vbox86p") ||
               Build.PRODUCT.contains("emulator") ||
               Build.PRODUCT.contains("simulator")
    }

    /**
     * Comprehensive tampering detection check.
     */
    fun detectTampering(context: Context): TamperingResult {
        val issues = mutableListOf<String>()

        if (isXposedActive()) {
            issues.add("Xposed Framework detected")
        }

        if (isFridaRunning()) {
            issues.add("Frida debugging detected")
        }

        if (isEmulator()) {
            issues.add("Running on emulator")
        }

        return TamperingResult(
            isTampered = issues.isNotEmpty(),
            issues = issues
        )
    }

    data class TamperingResult(
        val isTampered: Boolean,
        val issues: List<String>
    )
}
