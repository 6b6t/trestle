package net.blockhost.trestle.runtime

import java.io.File
import java.io.FileOutputStream

internal class AndroidGameOutputLog(
    private val file: File,
) {
    init {
        file.parentFile?.mkdirs()
        FileOutputStream(file, false).close()
    }

    @Synchronized
    fun append(line: String) {
        FileOutputStream(file, true).bufferedWriter().use { writer ->
            writer.appendLine(line)
        }
    }
}
