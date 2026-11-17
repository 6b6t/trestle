package net.blockhost.trestle.desktop

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopActivationTest {
    @Test
    fun `empty arguments activate the existing window`() {
        assertEquals(listOf(DesktopActivation.Show), DesktopActivationParser.parse(emptyList()))
    }

    @Test
    fun `files, links and window flags open the window instead of the command line`() {
        listOf(
            arrayOf("/home/player/Downloads/pack.mrpack"),
            arrayOf("trestle://launch/example-id"),
            arrayOf("--settings"),
            arrayOf("--launch-instance", "example-id"),
        ).forEach { arguments -> assertFalse(isCliInvocation(arguments), arguments.joinToString(" ")) }
        listOf(arrayOf("--list"), arrayOf("--install", "example-id"), arrayOf("--unknown")).forEach { arguments ->
            assertTrue(isCliInvocation(arguments), arguments.joinToString(" "))
        }
        assertFalse(isCliInvocation(emptyArray()))
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
                DesktopActivation.ShowInstance("example-id"),
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
