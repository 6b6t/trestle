package net.blockhost.trestle.instance

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream

/** A server from servers.dat with the raw NBT fields Trestle does not edit, written back unchanged. */
internal data class ServerEntry(val server: SavedServer, val extras: ByteArray = ByteArray(0)) {
    override fun equals(other: Any?): Boolean = other is ServerEntry && server == other.server && extras.contentEquals(other.extras)

    override fun hashCode(): Int = 31 * server.hashCode() + extras.contentHashCode()
}

/** Copies the bytes read between [startRecording] and [stopRecording], including skipped bytes. */
internal class RecordingInputStream(input: InputStream) : FilterInputStream(input) {
    private var recording: ByteArrayOutputStream? = null

    fun startRecording() {
        recording = ByteArrayOutputStream()
    }

    fun stopRecording(): ByteArray = recording?.toByteArray().also { recording = null } ?: ByteArray(0)

    override fun read(): Int = super.read().also { if (it >= 0) recording?.write(it) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) recording?.write(buffer, offset, it) }

    override fun skip(count: Long): Long {
        if (recording == null) return super.skip(count)
        val buffer = ByteArray(minOf(count, SKIP_BUFFER_SIZE.toLong()).toInt().coerceAtLeast(1))
        return read(buffer, 0, buffer.size).coerceAtLeast(0).toLong()
    }

    private companion object {
        const val SKIP_BUFFER_SIZE = 8_192
    }
}
