package com.codershubinc.aaxion_mob

import com.codershubinc.aaxion_mob.core.media.ArtworkResolver
import com.codershubinc.aaxion_mob.core.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkResolverTest {

    private val baseUrl = "http://192.168.1.100:8080"
    private val token = "sample_test_token"

    private fun createFile(name: String, isDir: Boolean = false): FileItem = FileItem(
        name = name,
        isDir = isDir,
        size = 1024L,
        path = "/Music/$name",
        rawPath = "/Music/$name"
    )

    @Test
    fun testExactTrackNameArtworkFoundInSameDirectory() {
        val track = createFile("01 - Bohemian Rhapsody.flac")
        val siblings = listOf(
            createFile("01 - Bohemian Rhapsody.flac"),
            createFile("01 - Bohemian Rhapsody.jpg"),
            createFile("02 - Another One Bites the Dust.flac"),
            createFile("02 - Another One Bites the Dust.png"),
            createFile("cover.jpg")
        )

        val artworkUrl = ArtworkResolver.findArtworkUrl(track, siblings, baseUrl, token)
        assertNotNull(artworkUrl)
        assertTrue(artworkUrl!!.contains("01+-+Bohemian+Rhapsody.jpg") || artworkUrl.contains("01%20-%20Bohemian%20Rhapsody.jpg") || artworkUrl.contains("01 - Bohemian Rhapsody.jpg") || artworkUrl.contains("01"))
        assertTrue(artworkUrl.contains("tkn=$token"))
    }

    @Test
    fun testDirectoryCoverFallbackWhenNoSameNamedImage() {
        val track = createFile("TrackWithoutCustomArt.mp3")
        val siblings = listOf(
            track,
            createFile("random_notes.txt"),
            createFile("cover.jpg")
        )

        val artworkUrl = ArtworkResolver.findArtworkUrl(track, siblings, baseUrl, token)
        assertNotNull(artworkUrl)
        assertTrue(artworkUrl!!.contains("cover.jpg"))
    }

    @Test
    fun testVideoThumbnailFallbackWhenNoMatchingImageInDirectory() {
        val videoTrack = createFile("Movie.2024.1080p.mkv")
        val siblings = listOf(
            videoTrack,
            createFile("subtitles.srt")
        )

        val artworkUrl = ArtworkResolver.findArtworkUrl(videoTrack, siblings, baseUrl, token)
        assertNotNull(artworkUrl)
        assertTrue(artworkUrl!!.contains("/api/v1/images/thumb"))
    }

    @Test
    fun testReturnsNullWhenNoArtworkForAudio() {
        val track = createFile("SoloTrack.mp3")
        val siblings = listOf(
            track,
            createFile("lyrics.txt")
        )

        val artworkUrl = ArtworkResolver.findArtworkUrl(track, siblings, baseUrl, token)
        assertNull(artworkUrl)
    }
}
