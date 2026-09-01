package net.blockhost.trestle.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.GzipSink
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem

class GameFileBrowserFilesTest {
    @Test
    fun listsFoldersFirstAndClassifiesFiles() {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        val root = "/instances/demo".toPath()
        fileSystem.createDirectories(root / "game/config")
        fileSystem.write(root / "game/options.txt") { writeUtf8("fov:70") }
        fileSystem.write(root / "game/screenshot.png") { writeUtf8("png") }
        fileSystem.write(root / "game/mod.jar") { writeUtf8("jar") }

        val browser = GameFileBrowserFiles(root.toString(), fileSystem)
        val directory = browser.list((root / "game").toString())

        assertEquals(listOf("config", "mod.jar", "options.txt", "screenshot.png"), directory.entries.map { it.name })
        assertEquals(
            listOf(GameFileType.DIRECTORY, GameFileType.ARCHIVE, GameFileType.TEXT, GameFileType.IMAGE),
            directory.entries.map { it.type },
        )
        assertEquals(listOf("game"), directory.relativeSegments)
    }

    @Test
    fun rejectsPathsAndSymlinksOutsideTheInstance() {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        val root = "/instances/demo".toPath()
        fileSystem.createDirectories(root)
        fileSystem.createDirectories("/outside".toPath())
        fileSystem.write("/outside/secret.txt".toPath()) { writeUtf8("secret") }
        fileSystem.createSymlink(root / "escape", "/outside".toPath())
        val browser = GameFileBrowserFiles(root.toString(), fileSystem)

        assertFailsWith<IllegalArgumentException> { browser.locate("/outside/secret.txt") }
        assertTrue(browser.list(root.toString()).entries.none { it.name == "escape" })
    }

    @Test
    fun editsConfigurationFilesAtomically() {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        val root = "/instances/demo".toPath()
        val configuration = root / "game/config/example.toml"
        fileSystem.createDirectories(requireNotNull(configuration.parent))
        fileSystem.write(configuration) { writeUtf8("enabled = false\n") }
        val browser = GameFileBrowserFiles(root.toString(), fileSystem)

        val opened = browser.readText(configuration.toString())
        assertTrue(opened.editable)
        val saved = browser.saveText(opened, "enabled = true\n")

        assertEquals("enabled = true\n", saved.text)
        assertFalse(fileSystem.exists(requireNotNull(configuration.parent) / ".example.toml.trestle-edit.tmp"))
        assertEquals("enabled = true\n", fileSystem.read(configuration) { readUtf8() })
    }

    @Test
    fun previewsCompressedLogsWithoutMakingThemEditable() {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        val root = "/instances/demo".toPath()
        val log = root / "game/logs/latest.log.gz"
        fileSystem.createDirectories(requireNotNull(log.parent))
        GzipSink(fileSystem.sink(log)).buffer().use { sink ->
            sink.writeUtf8("first line\nsecond line\nlast line\n")
        }
        val browser = GameFileBrowserFiles(root.toString(), fileSystem)

        val opened = browser.readText(log.toString())

        assertEquals("first line\nsecond line\nlast line\n", opened.text)
        assertFalse(opened.editable)
        assertFalse(opened.truncated)
    }

    @Test
    fun showsOnlyTheTailOfLargeLogs() {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        val root = "/instances/demo".toPath()
        val log = root / "game/logs/latest.log"
        fileSystem.createDirectories(requireNotNull(log.parent))
        fileSystem.write(log) {
            repeat(70_000) { line -> writeUtf8("line $line\n") }
            writeUtf8("the final message\n")
        }
        val browser = GameFileBrowserFiles(root.toString(), fileSystem)

        val opened = browser.readText(log.toString())

        assertTrue(opened.truncated)
        assertFalse(opened.editable)
        assertTrue(opened.text.endsWith("the final message\n"))
        assertTrue(opened.text.encodeToByteArray().size < 256 * 1024)
    }
}
