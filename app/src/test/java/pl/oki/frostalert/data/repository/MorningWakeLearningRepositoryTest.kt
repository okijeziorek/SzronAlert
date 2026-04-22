package pl.oki.frostalert.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.DEFAULT_WINDOW_END_MINUTE
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.DEFAULT_WINDOW_START_MINUTE
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.MAX_HISTORY_ENTRIES
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.WINDOW_HALF_AFTER_MINUTES
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.WINDOW_HALF_BEFORE_MINUTES
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.appendWakeMinute
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.computeMedianFromJson
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.computeMorningWindow
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository.Companion.parseHistoryJson

class MorningWakeLearningRepositoryTest {

    // ── parseHistoryJson ──────────────────────────────────────────────────────

    @Test
    fun `parseHistoryJson returns empty list for empty JSON`() {
        assertEquals(emptyList<Int>(), parseHistoryJson("[]"))
    }

    @Test
    fun `parseHistoryJson returns empty list for blank string`() {
        assertEquals(emptyList<Int>(), parseHistoryJson(""))
    }

    @Test
    fun `parseHistoryJson parses single entry`() {
        assertEquals(listOf(450), parseHistoryJson("[450]"))
    }

    @Test
    fun `parseHistoryJson parses multiple entries`() {
        assertEquals(listOf(450, 480, 420), parseHistoryJson("[450,480,420]"))
    }

    @Test
    fun `parseHistoryJson ignores malformed tokens`() {
        assertEquals(listOf(450, 480), parseHistoryJson("[450,abc,480]"))
    }

    // ── computeMedianFromJson ─────────────────────────────────────────────────

    @Test
    fun `computeMedianFromJson returns null for empty history`() {
        assertNull(computeMedianFromJson("[]"))
    }

    @Test
    fun `computeMedianFromJson returns single value when list has one element`() {
        assertEquals(450, computeMedianFromJson("[450]"))
    }

    @Test
    fun `computeMedianFromJson returns correct median for odd-sized list`() {
        // Sorted: 400, 450, 500 → median = 450
        assertEquals(450, computeMedianFromJson("[500,400,450]"))
    }

    @Test
    fun `computeMedianFromJson returns average of two middle values for even-sized list`() {
        // Sorted: 400, 450, 460, 500 → median = (450+460)/2 = 455
        assertEquals(455, computeMedianFromJson("[500,400,450,460]"))
    }

    @Test
    fun `computeMedianFromJson handles outliers without crashing`() {
        // Very early (1 min) and very late (1439 min) outliers
        val json = "[480,1439,1,480,480]"
        val median = computeMedianFromJson(json)
        assertEquals(480, median)
    }

    @Test
    fun `computeMedianFromJson is stable for all-same values`() {
        assertEquals(360, computeMedianFromJson("[360,360,360,360]"))
    }

    // ── appendWakeMinute ──────────────────────────────────────────────────────

    @Test
    fun `appendWakeMinute adds entry to empty history`() {
        val result = appendWakeMinute("[]", 450, 14)
        assertEquals(listOf(450), parseHistoryJson(result))
    }

    @Test
    fun `appendWakeMinute appends to existing history`() {
        val result = appendWakeMinute("[420,430]", 450, 14)
        assertEquals(listOf(420, 430, 450), parseHistoryJson(result))
    }

    @Test
    fun `appendWakeMinute trims oldest entries when maxDays exceeded`() {
        val initial = (1..5).joinToString(",", "[", "]") { (400 + it).toString() }
        val result = appendWakeMinute(initial, 410, maxDays = 5)
        val list = parseHistoryJson(result)
        assertEquals(5, list.size)
        assertFalse("Oldest entry should be removed", list.contains(401))
        assertTrue("Newest entry should be present", list.contains(410))
    }

    @Test
    fun `appendWakeMinute respects MAX_HISTORY_ENTRIES cap`() {
        val history = (1..MAX_HISTORY_ENTRIES).joinToString(",", "[", "]") { "480" }
        val result = appendWakeMinute(history, 490, maxDays = MAX_HISTORY_ENTRIES)
        assertEquals(MAX_HISTORY_ENTRIES, parseHistoryJson(result).size)
    }

    // ── computeMorningWindow ──────────────────────────────────────────────────

    @Test
    fun `computeMorningWindow returns correct window for typical median`() {
        val median = 480 // 08:00
        val (start, end) = computeMorningWindow(median)
        assertEquals(median - WINDOW_HALF_BEFORE_MINUTES, start)
        assertEquals(median + WINDOW_HALF_AFTER_MINUTES, end)
    }

    @Test
    fun `computeMorningWindow clamps start to 0 for very early median`() {
        val (start, _) = computeMorningWindow(30) // 00:30
        assertEquals(0, start)
    }

    @Test
    fun `computeMorningWindow clamps end to 23h59m for very late median`() {
        val (_, end) = computeMorningWindow(23 * 60 + 30) // 23:30
        assertEquals(23 * 60 + 59, end)
    }

    @Test
    fun `default window constants are 06h00 to 10h00`() {
        assertEquals(360, DEFAULT_WINDOW_START_MINUTE)
        assertEquals(600, DEFAULT_WINDOW_END_MINUTE)
    }

    // ── isWithinMorningWindow (via computed window) ───────────────────────────

    @Test
    fun `minute inside default window is accepted`() {
        assertTrue(480 in DEFAULT_WINDOW_START_MINUTE..DEFAULT_WINDOW_END_MINUTE) // 08:00
    }

    @Test
    fun `minute before default window is rejected`() {
        assertFalse(300 in DEFAULT_WINDOW_START_MINUTE..DEFAULT_WINDOW_END_MINUTE) // 05:00
    }

    @Test
    fun `minute after default window is rejected`() {
        assertFalse(660 in DEFAULT_WINDOW_START_MINUTE..DEFAULT_WINDOW_END_MINUTE) // 11:00
    }

    // ── Learning convergence scenario ─────────────────────────────────────────

    @Test
    fun `median converges toward actual wake time over multiple days`() {
        // Simulate 10 days of waking at ~07:30 (450 min)
        var json = "[]"
        repeat(10) { json = appendWakeMinute(json, 450 + (-5..5).random(), 14) }
        val median = computeMedianFromJson(json)!!
        assertTrue("Median $median should be near 450", median in 440..460)
    }
}
