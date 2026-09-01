package net.blockhost.trestle.runtime

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal class AndroidGameOutputLog(
    private val file: File,
) {
    init {
        file.parentFile?.mkdirs()
        FileOutputStream(file, false).close()
    }

    fun tailer(): AndroidGameOutputTailer = AndroidGameOutputTailer(file)
}

internal class AndroidGameOutputTailer(file: File) : Closeable {
    private val input = FileInputStream(file)
    private val pendingLine = ByteArrayOutputStream()

    @Synchronized
    fun readAvailableLines(includePartialLine: Boolean = false): List<String> = buildList {
        val channel = input.channel
        var remaining = (channel.size() - channel.position()).coerceAtMost(MAX_READ_BYTES.toLong()).toInt()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            val count = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (count <= 0) break
            remaining -= count
            for (index in 0 until count) {
                if (buffer[index] == '\n'.code.toByte()) {
                    add(takePendingLine())
                } else {
                    pendingLine.write(buffer[index].toInt())
                }
            }
        }
        if (includePartialLine && pendingLine.size() > 0) add(takePendingLine())
    }

    @Synchronized
    override fun close() {
        input.close()
    }

    private fun takePendingLine(): String {
        val bytes = pendingLine.toByteArray()
        pendingLine.reset()
        val length = if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.size - 1 else bytes.size
        return bytes.decodeToString(0, length)
    }

    private companion object {
        const val MAX_READ_BYTES = 256 * 1024
    }
}
