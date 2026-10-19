package com.codershubinc.aaxion_mob

import android.net.Uri
import com.codershubinc.aaxion_mob.core.network.ApiEndpoints
import com.codershubinc.aaxion_mob.core.upload.UploadStatus
import com.codershubinc.aaxion_mob.core.upload.UploadTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadRoutingTest {

    private fun isLocalTestHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1") return true
        if (host.startsWith("192.168.")) return true
        if (host.startsWith("10.")) return true
        if (host.startsWith("172.")) {
            val parts = host.split(".")
            if (parts.size >= 2) {
                val second = parts[1].toIntOrNull()
                if (second != null && second in 16..31) return true
            }
        }
        return false
    }

    @Test
    fun testLocalIpVsTunnelDetection() {
        assertTrue(isLocalTestHost("192.168.1.50"))
        assertTrue(isLocalTestHost("127.0.0.1"))
        assertTrue(isLocalTestHost("localhost"))
        assertTrue(isLocalTestHost("10.0.2.2"))
        assertTrue(isLocalTestHost("172.20.10.4"))

        assertFalse(isLocalTestHost("aaxion-tunnel.trycloudflare.com"))
        assertFalse(isLocalTestHost("myserver.ngrok.io"))
        assertFalse(isLocalTestHost("cloud.example.org"))
        assertFalse(isLocalTestHost("8.8.8.8"))
    }

    @Test
    fun testUploadTaskProgressAndFormatting() {
        val task = UploadTask(
            name = "video.mp4",
            uri = null,
            sizeBytes = 100 * 1024 * 1024L, // 100 MB
            targetDir = "/home/user/Movies",
            serverUri = "http://192.168.1.50:8080",
            token = "token_abc",
            status = UploadStatus.UPLOADING,
            bytesUploaded = 50 * 1024 * 1024L, // 50 MB (50%)
            speedBytesPerSec = 2 * 1024 * 1024L // 2 MB/s
        )

        assertEquals(0.5f, task.progress, 0.001f)
        assertEquals(50, task.progressPercent)
        assertEquals("2.00 MB/s", task.formattedSpeed)
        assertEquals("100.0 MB", task.formattedSize)
        assertEquals("50.0 MB", task.formattedUploaded)
    }

    @Test
    fun testUploadEndpointsMatchSpecification() {
        assertEquals("/api/v1/files/directory/create", ApiEndpoints.FILES.CREATE_DIR)
        assertEquals("/api/v1/files/upload", ApiEndpoints.FILES.UPLOAD)
        assertEquals("/api/v1/files/upload/chunk/start", ApiEndpoints.CHUNKS.START)
        assertEquals("/api/v1/files/upload/chunk", ApiEndpoints.CHUNKS.UPLOAD)
        assertEquals("/api/v1/files/upload/chunk/complete", ApiEndpoints.CHUNKS.COMPLETE)
    }

    @Test
    fun testTargetDirMatchingForRefresh() {
        fun shouldRefresh(currentDir: String, uploadDir: String): Boolean {
            val cur = currentDir.trimEnd('/')
            val target = uploadDir.trimEnd('/')
            return cur.equals(target, ignoreCase = true)
        }

        assertTrue(shouldRefresh("/home/user/Movies", "/home/user/Movies"))
        assertTrue(shouldRefresh("/home/user/Movies/", "/home/user/Movies"))
        assertTrue(shouldRefresh("/home/user/Movies", "/home/user/Movies/"))
        assertTrue(shouldRefresh("/", "/"))
        assertTrue(shouldRefresh("", "/"))
        assertTrue(shouldRefresh("/", ""))
        assertFalse(shouldRefresh("/home/user/Movies", "/home/user/Downloads"))
    }
}

