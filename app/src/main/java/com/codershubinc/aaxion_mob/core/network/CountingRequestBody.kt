package com.codershubinc.aaxion_mob.core.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException

/**
 * Custom OkHttp RequestBody decorator that reports upload progress in real-time.
 */
class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, totalBytes: Long, speedBytesPerSec: Long) -> Unit,
    private val isCancelled: () -> Boolean = { false }
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten: Long = 0L
        private var lastTimeNs = System.nanoTime()
        private var lastBytes = 0L
        private var speedBytesPerSec = 0L

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            if (isCancelled()) {
                throw IOException("Upload cancelled by user")
            }
            super.write(source, byteCount)
            bytesWritten += byteCount

            val now = System.nanoTime()
            val elapsedSec = (now - lastTimeNs) / 1_000_000_000.0
            if (elapsedSec >= 0.25 || bytesWritten == contentLength()) {
                val bytesSinceLast = bytesWritten - lastBytes
                val currentSpeed = if (elapsedSec > 0) (bytesSinceLast / elapsedSec).toLong() else 0L
                speedBytesPerSec = if (speedBytesPerSec == 0L) {
                    currentSpeed
                } else {
                    ((speedBytesPerSec * 0.7) + (currentSpeed * 0.3)).toLong()
                }
                lastBytes = bytesWritten
                lastTimeNs = now
                onProgress(bytesWritten, contentLength(), speedBytesPerSec)
            }
        }
    }
}
