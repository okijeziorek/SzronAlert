package pl.oki.frostalert.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.R

class MainScreenTabsTest {

    @Test
    fun buildMainTabs_releaseMode_hasSixTabsWithoutDebug() {
        val tabs = buildMainTabs(isDebugBuild = false)

        assertEquals(6, tabs.size)
        assertFalse(tabs.any { it.labelRes == R.string.tab_debug })
        assertEquals(listOf(0, 1, 2, 3, 4, 5), tabs.map { it.index })
    }

    @Test
    fun buildMainTabs_debugMode_addsDebugTab() {
        val tabs = buildMainTabs(isDebugBuild = true)

        assertEquals(7, tabs.size)
        assertTrue(tabs.any { it.index == 6 && it.labelRes == R.string.tab_debug })
    }
}
