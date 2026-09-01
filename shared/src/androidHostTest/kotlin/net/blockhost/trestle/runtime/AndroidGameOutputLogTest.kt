package net.blockhost.trestle.runtime

import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidGameOutputLogTest {
    @Test
    fun resetsThePreviousLaunchAndStreamsAppendedOutput() {
        val root = Files.createTempDirectory("trestle-game-output-test").toFile()
        try {
            val logFile = root.resolve("game/.trestle/logs/latest.log").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("old launch\n")
            }

            val outputLog = AndroidGameOutputLog(logFile)
            outputLog.tailer().use { tailer ->
                FileOutputStream(logFile, true).use { output ->
                    output.write("Starting Minecraft\nNative boot".encodeToByteArray())
                }
                assertEquals(listOf("Starting Minecraft"), tailer.readAvailableLines())

                FileOutputStream(logFile, true).use { output ->
                    output.write("strap failed\r\nNo newline".encodeToByteArray())
                }
                assertEquals(listOf("Native bootstrap failed"), tailer.readAvailableLines())
                assertEquals(listOf("No newline"), tailer.readAvailableLines(includePartialLine = true))
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
