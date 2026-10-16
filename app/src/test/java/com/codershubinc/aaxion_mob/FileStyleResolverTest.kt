package com.codershubinc.aaxion_mob

import androidx.compose.ui.graphics.Color
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.IconPackTheme
import com.codershubinc.aaxion_mob.ui.designsystem.computeFileStyle
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileStyleResolverTest {

    private val accentColor = Color(0xFF6366F1)
    private val iconPack = IconPackTheme.MINIMAL_ROUNDED

    private fun createDir(name: String): FileItem = FileItem(
        name = name,
        isDir = true,
        size = 4096L,
        path = "/media/$name",
        rawPath = "/media/$name"
    )

    @Test
    fun testMediaReleaseFoldersDoNotGetSpecialEmblem() {
        val releases = listOf(
            "Black.Clover.S01.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com",
            "Breaking.Bad.S05.1080p.10Bit.BluRay.Hindi.ORG.2.0-English.5.1.HEVC.x265-HDHub4u.Tv",
            "Rick.and.Morty.S09.1080p.AMZN.WEB-DL.English.DDP5.1.H.265-4kHdHub.Com",
            "Game.of.Thrones.S08.2160p.UHD.HDR.BluRay.x265",
            "Stranger.Things.Season.4.720p.WEBRip.x264"
        )

        for (rel in releases) {
            val style = computeFileStyle(createDir(rel), accentColor, iconPack)
            assertTrue("Expected isFolder to be true for $rel", style.isFolder)
            assertNull("Media release folder '$rel' should NOT have an emblem", style.folderEmblem)
        }
    }

    @Test
    fun testStandardFoldersGetSpecialEmblem() {
        val standardDirs = listOf(
            "Downloads",
            "downloads",
            "My Downloads",
            "Videos",
            "Movies",
            "TV Series",
            "Music",
            "Pictures",
            "DCIM",
            "Documents",
            "Desktop",
            "docker"
        )

        for (dir in standardDirs) {
            val style = computeFileStyle(createDir(dir), accentColor, iconPack)
            assertTrue("Expected isFolder to be true for $dir", style.isFolder)
            assertNotNull("Standard folder '$dir' should have a special emblem or custom icon", style.folderEmblem ?: style.icon)
        }
    }
}
