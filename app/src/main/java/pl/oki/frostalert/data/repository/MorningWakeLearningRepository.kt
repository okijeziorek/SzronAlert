package pl.oki.frostalert.data.repository

import android.util.Log
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.data.local.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the "learning when the user wakes up" logic for the morning brief feature.
 *
 * Pure-logic helpers (companion object) are separated from DataStore I/O (instance methods)
 * so they can be tested without Android dependencies.
 */
@Singleton
class MorningWakeLearningRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {

    companion object {
        private const val TAG = "MorningWakeLearning"

        /** Default morning window: 06:00–10:00. */
        const val DEFAULT_WINDOW_START_MINUTE = 360
        const val DEFAULT_WINDOW_END_MINUTE = 600

        /** Window is [median - BEFORE, median + AFTER] once learning kicks in. */
        const val WINDOW_HALF_BEFORE_MINUTES = 90
        const val WINDOW_HALF_AFTER_MINUTES = 60

        /** Absolute cap for how many history entries are kept. */
        const val MAX_HISTORY_ENTRIES = 30

        /**
         * Appends [minuteOfDay] to the existing JSON history and trims to [maxDays] entries.
         * Format: "[450,480,460]" (minutes from midnight, newest at the end).
         */
        fun appendWakeMinute(existingJson: String, minuteOfDay: Int, maxDays: Int): String {
            val list = parseHistoryJson(existingJson).toMutableList()
            list.add(minuteOfDay)
            val trimmed = if (list.size > maxDays) list.drop(list.size - maxDays) else list
            return trimmed.joinToString(",", "[", "]")
        }

        /**
         * Computes the median wake-minute from the stored JSON history.
         * Returns null if the history is empty.
         */
        fun computeMedianFromJson(json: String): Int? {
            val list = parseHistoryJson(json)
            if (list.isEmpty()) return null
            val sorted = list.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid]
            else (sorted[mid - 1] + sorted[mid]) / 2
        }

        /**
         * Derives a [Pair] of (windowStartMinute, windowEndMinute) from the learned median.
         */
        fun computeMorningWindow(medianMinute: Int): Pair<Int, Int> {
            val start = (medianMinute - WINDOW_HALF_BEFORE_MINUTES).coerceAtLeast(0)
            val end = (medianMinute + WINDOW_HALF_AFTER_MINUTES).coerceAtMost(23 * 60 + 59)
            return Pair(start, end)
        }

        /**
         * Parses a JSON string of the form "[450,480,460]" into a list of integers.
         * Malformed tokens are silently skipped.
         */
        fun parseHistoryJson(json: String): List<Int> {
            return try {
                val trimmed = json.trim()
                if (trimmed == "[]" || trimmed.isBlank()) return emptyList()
                trimmed.trim('[', ']').split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse wake history JSON: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Records the first unlock of [epochDay] at [minuteOfDay].
     * Updates history, recomputes median, and adjusts the morning window.
     *
     * @return true if this was the *first* unlock recorded for this epoch day; false otherwise.
     */
    suspend fun recordFirstUnlockOfDay(epochDay: Long, minuteOfDay: Int): Boolean {
        val prefs = settingsDataStore.userPreferencesFlow.first()
        if (prefs.morningLastUnlockEpochDay == epochDay) {
            Log.d(TAG, "Unlock already recorded for epoch day $epochDay — skipping")
            return false
        }

        settingsDataStore.updateMorningLastUnlockEpochDay(epochDay)

        val maxDays = prefs.morningLearningDays.coerceIn(3, MAX_HISTORY_ENTRIES)
        val updatedJson = appendWakeMinute(prefs.morningWakeHistoryJson, minuteOfDay, maxDays)
        settingsDataStore.updateMorningWakeHistoryJson(updatedJson)

        val newMedian = computeMedianFromJson(updatedJson)
        if (newMedian != null) {
            settingsDataStore.updateMorningMedianWakeMinute(newMedian)
            val (start, end) = computeMorningWindow(newMedian)
            settingsDataStore.updateMorningWindowStartMinute(start)
            settingsDataStore.updateMorningWindowEndMinute(end)
            Log.d(TAG, "Updated morning window: ${start / 60}:${start % 60}–${end / 60}:${end % 60} (median ${newMedian / 60}:${newMedian % 60})")
        }

        return true
    }

    /**
     * Returns true if [currentMinuteOfDay] falls within the stored morning window.
     * Falls back to the default 06:00–10:00 window if no learning data exists yet.
     */
    suspend fun isWithinMorningWindow(currentMinuteOfDay: Int): Boolean {
        val prefs = settingsDataStore.userPreferencesFlow.first()
        return currentMinuteOfDay in prefs.morningWindowStartMinute..prefs.morningWindowEndMinute
    }

    /**
     * Returns true if a morning brief notification has NOT been sent today
     * (i.e., [todayEpochDay] differs from the stored last-notification epoch day).
     */
    suspend fun shouldScheduleBriefToday(todayEpochDay: Long): Boolean {
        val prefs = settingsDataStore.userPreferencesFlow.first()
        return prefs.morningLastNotificationEpochDay != todayEpochDay
    }

    /** Records the timestamp when a morning brief was last scheduled. */
    suspend fun recordScheduled(ms: Long) {
        settingsDataStore.updateMorningLastScheduledAtMs(ms)
    }

    /** Marks today's epoch day as having a morning brief notification sent. */
    suspend fun recordNotificationSent(epochDay: Long) {
        settingsDataStore.updateMorningLastNotificationEpochDay(epochDay)
    }
}
