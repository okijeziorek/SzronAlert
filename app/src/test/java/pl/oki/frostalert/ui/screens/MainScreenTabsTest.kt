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

    @Test
    fun buildMainTabs_contentValues_matchTabOrder() {
        val tabs = buildMainTabs(isDebugBuild = false)
        val contents = tabs.map { it.content }

        assertEquals(
            listOf(
                TabContent.HOME,
                TabContent.SETTINGS,
                TabContent.HISTORY,
                TabContent.TREND,
                TabContent.GARDEN,
                TabContent.MAP
            ),
            contents
        )
    }

    @Test
    fun buildMainTabs_debugTab_hasDebugContent() {
        val tabs = buildMainTabs(isDebugBuild = true)
        val debugTab = tabs.first { it.index == 6 }

        assertEquals(TabContent.DEBUG, debugTab.content)
    }

    @Test
    fun buildMainTabs_indicesAreSequential() {
        val tabs = buildMainTabs(isDebugBuild = true)
        assertEquals(tabs.indices.toList(), tabs.map { it.index })
    }
}
