package com.codershubinc.aaxion_mob

import com.codershubinc.aaxion_mob.core.model.AddTrackResponse
import com.codershubinc.aaxion_mob.core.model.MusicTrack
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.core.network.ApiEndpoints
import com.codershubinc.aaxion_mob.core.network.buildArtworkUrl
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicIntegrationTest {

    private val baseUrl = "http://192.168.1.50:8080"
    private val token = "secure_aaxion_token_xyz"

    private fun sampleTrack(id: Long = 42L): MusicTrack = MusicTrack(
        id = id,
        title = "Never Gonna Give You Up",
        artist = "Rick Astley",
        album = "Whenever You Need Somebody",
        duration = 213.5,
        releaseYear = 1987,
        filePath = "/home/user/.aaxion/music/dQw4w9WgXcQ.mp3",
        ytUri = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        imagePath = "/home/user/.aaxion/music/dQw4w9WgXcQ.png",
        size = 5123984L,
        createdAt = "2026-10-17T17:20:00Z"
    )

    @Test
    fun testMusicTrackToFileItemConversion() {
        val track = sampleTrack()
        val fileItem = track.toFileItem()

        assertFalse(fileItem.isDir)
        assertEquals(track.size, fileItem.size)
        assertEquals(track.filePath, fileItem.path)
        assertEquals(track.filePath, fileItem.rawPath)
        assertTrue(fileItem.isAudio)
        assertEquals("mp3", fileItem.extension)
        assertEquals("Rick Astley - Never Gonna Give You Up.mp3", fileItem.name)
    }

    @Test
    fun testMusicTrackDisplayFallbacks() {
        val track = MusicTrack(
            id = 7L,
            title = "",
            artist = "",
            album = "",
            duration = 180.0,
            releaseYear = 0,
            filePath = "/music/UnknownSong.flac",
            ytUri = "",
            imagePath = "",
            size = 1000L,
            createdAt = ""
        )

        assertEquals("UnknownSong", track.displayTitle)
        assertEquals("Unknown Artist", track.displayArtist)
    }

    @Test
    fun testMusicStreamUrlBuilder() {
        val track = sampleTrack(15L)
        val streamUrl = track.buildStreamUrl(baseUrl, token)

        assertEquals(
            "http://192.168.1.50:8080/api/v1/music/stream?id=15&tkn=secure_aaxion_token_xyz",
            streamUrl
        )
    }

    @Test
    fun testMusicArtworkUrlBuilder() {
        val track = sampleTrack()
        val artworkUrl = track.buildArtworkUrl(baseUrl, token)

        assertTrue(artworkUrl.startsWith("http://192.168.1.50:8080/api/v1/images/view?path="))
        assertTrue(artworkUrl.contains("tkn=secure_aaxion_token_xyz"))
    }

    @Test
    fun testApiEndpointsMatchServerSpecification() {
        assertEquals("/api/v1/music", ApiEndpoints.MUSIC.LIST)
        assertEquals("/api/v1/music/get", ApiEndpoints.MUSIC.GET)
        assertEquals("/api/v1/music/search", ApiEndpoints.MUSIC.SEARCH)
        assertEquals("/api/v1/music/add", ApiEndpoints.MUSIC.ADD)
        assertEquals("/api/v1/music/update", ApiEndpoints.MUSIC.UPDATE)
        assertEquals("/api/v1/music/stream", ApiEndpoints.MUSIC.STREAM)
        assertEquals("/api/v1/system/storage", ApiEndpoints.SYSTEM.STORAGE)
    }

    @Test
    fun testAddTrackResponseDeserialization() {
        val json = """
            {
              "status": "success",
              "message": "Downloaded 1 of 1 tracks",
              "total": 1,
              "downloaded": 1,
              "failed": 0,
              "tracks": [
                {
                  "id": 1,
                  "title": "Never Gonna Give You Up",
                  "artist": "Rick Astley",
                  "album": "Whenever You Need Somebody",
                  "duration": 213,
                  "releaseYear": 1987,
                  "filePath": "/home/swap/.aaxion/music/dQw4w9WgXcQ.mp3",
                  "ytUri": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                  "imagePath": "/home/swap/.aaxion/music/dQw4w9WgXcQ.png",
                  "size": 5123984,
                  "createdAt": "2026-10-17T17:20:00Z"
                }
              ]
            }
        """.trimIndent()

        val response = Gson().fromJson(json, AddTrackResponse::class.java)
        assertEquals("success", response.status)
        assertEquals(1, response.total)
        assertEquals(1, response.downloaded)
        assertEquals(0, response.failed)
        assertEquals(1, response.tracks.size)
        assertEquals("Never Gonna Give You Up", response.tracks[0].title)
        assertEquals(1987, response.tracks[0].releaseYear)
    }
}
