package net.blockhost.trestle.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidGameOutputLogTest {
    @Test
    fun resetsThePreviousLaunchAndPersistsEveryLine() {
        val root = Files.createTempDirectory("trestle-game-output-test").toFile()
        try {
            val logFile = root.resolve("game/.trestle/logs/latest.log").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("old launch\n")
            }

            val outputLog = AndroidGameOutputLog(logFile)
            outputLog.append("Starting Minecraft")
            outputLog.append("Native bootstrap failed")

            assertEquals(
                "Starting Minecraft\nNative bootstrap failed\n",
                logFile.readText().replace("\r\n", "\n"),
            )
        } finally {
            root.deleteRecursively()
        }
    }
}
