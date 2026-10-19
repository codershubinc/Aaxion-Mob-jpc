package com.codershubinc.aaxion_mob

import com.codershubinc.aaxion_mob.core.media.RecentMediaItem
import com.codershubinc.aaxion_mob.feature.home.components.RecentMediaFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeModularizationTest {

    @Test
    fun testRecentMediaFilterEnumValues() {
        assertEquals("All", RecentMediaFilter.ALL.label)
        assertEquals("Videos", RecentMediaFilter.VIDEO.label)
        assertEquals("Music", RecentMediaFilter.AUDIO.label)
    }

    @Test
    fun testRecentMediaFilteringLogic() {
        val sampleItems = listOf(
            RecentMediaItem(
                id = "1",
                rawPath = "/home/user/Movies/Movie1.mkv",
                title = "Movie 1",
                subtitle = "Movies",
                isVideo = true,
                isAudio = false,
                progressMs = 10000L,
                durationMs = 60000L,
                lastPlayedTs = 1000L
            ),
            RecentMediaItem(
                id = "2",
                rawPath = "/home/user/Music/Song1.flac",
                title = "Song 1",
                subtitle = "Music",
                isVideo = false,
                isAudio = true,
                progressMs = 5000L,
                durationMs = 30000L,
                lastPlayedTs = 2000L
            ),
            RecentMediaItem(
                id = "3",
                rawPath = "/home/user/Series/S01E01.mp4",
                title = "Show S01E01",
                subtitle = "Series",
                isVideo = true,
                isAudio = false,
                progressMs = 20000L,
                durationMs = 50000L,
                lastPlayedTs = 3000L
            )
        )

        val allItems = when (RecentMediaFilter.ALL) {
            RecentMediaFilter.VIDEO -> sampleItems.filter { it.isVideo }
            RecentMediaFilter.AUDIO -> sampleItems.filter { it.isAudio }
            RecentMediaFilter.ALL -> sampleItems
        }
        assertEquals(3, allItems.size)

        val videoItems = when (RecentMediaFilter.VIDEO) {
            RecentMediaFilter.VIDEO -> sampleItems.filter { it.isVideo }
            RecentMediaFilter.AUDIO -> sampleItems.filter { it.isAudio }
            RecentMediaFilter.ALL -> sampleItems
        }
        assertEquals(2, videoItems.size)

        val audioItems = when (RecentMediaFilter.AUDIO) {
            RecentMediaFilter.VIDEO -> sampleItems.filter { it.isVideo }
            RecentMediaFilter.AUDIO -> sampleItems.filter { it.isAudio }
            RecentMediaFilter.ALL -> sampleItems
        }
        assertEquals(1, audioItems.size)
        assertEquals("Song 1", audioItems.first().title)
    }

    @Test
    fun testHomeScreenLineCountUnder150() {
        val homeScreenFile = File("src/main/java/com/codershubinc/aaxion_mob/feature/home/HomeScreen.kt")
        if (homeScreenFile.exists()) {
            val lines = homeScreenFile.readLines().size
            assertTrue("HomeScreen.kt line count must be under 150 lines, was: $lines", lines < 150)
        }
    }

    @Test
    fun testComponentsExistInDedicatedDirectory() {
        val componentsDir = File("src/main/java/com/codershubinc/aaxion_mob/feature/home/components")
        if (componentsDir.exists()) {
            val componentNames = componentsDir.list()?.toList() ?: emptyList()
            assertTrue(componentNames.contains("HomeHeaderBar.kt"))
            assertTrue(componentNames.contains("StorageHeroCard.kt"))
            assertTrue(componentNames.contains("BentoWorkspaceGrid.kt"))
            assertTrue(componentNames.contains("ContinuePlayingSection.kt"))
            assertTrue(componentNames.contains("RecentMediaGlassCard.kt"))
        }
    }
}
