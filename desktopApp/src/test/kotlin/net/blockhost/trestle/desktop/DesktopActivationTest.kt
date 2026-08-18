package net.blockhost.trestle.desktop

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopActivationTest {
    @Test
    fun `empty arguments activate the existing window`() {
        assertEquals(listOf(DesktopActivation.Show), DesktopActivationParser.parse(emptyList()))
    }

    @Test
    fun `parser groups supported files and keeps deep link commands`() {
        val directory = createTempDirectory("trestle-activation-test")
        val mod = Files.createFile(directory.resolve("example.jar"))
        val pack = Files.createFile(directory.resolve("example.mrpack"))

        val activations = DesktopActivationParser.parse(
            listOf(mod.toString(), "trestle://settings", "trestle://launch/example-id", pack.toUri().toString()),
            directory,
        )

        assertEquals(
            listOf(
                DesktopActivation.ImportFiles(listOf(mod, pack)),
                DesktopActivation.OpenSettings,
                DesktopActivation.LaunchInstance("example-id"),
            ),
            activations,
        )
    }

    @Test
    fun `unsupported paths do not become imports`() {
        val directory = createTempDirectory("trestle-activation-test")
        Files.createFile(directory.resolve("notes.txt"))

        assertEquals(
            listOf(DesktopActivation.Show),
            DesktopActivationParser.parse(listOf("notes.txt"), directory),
        )
    }
}
