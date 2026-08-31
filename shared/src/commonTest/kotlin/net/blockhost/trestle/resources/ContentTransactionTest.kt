package net.blockhost.trestle.resources

import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.*

class ContentTransactionTest {
    @Test fun restoresChangesWithoutTouchingUserFiles() {
        val fs = FakeFileSystem()
        val root = "/instance".toPath()
        fs.createDirectories(root / "game")
        fs.createDirectories("/stage".toPath())
        fs.write(root / "game/old.jar") { write(byteArrayOf(1, 2)) }
        fs.write(root / "game/world.dat") { write(byteArrayOf(9)) }
        fs.write("/stage/new.jar".toPath()) { write(byteArrayOf(3, 4)) }
        val original = fs.sha256(root / "game/old.jar")
        val transaction = ContentTransaction(fs, root, root / ".trestle/backup")
        transaction.apply(mapOf("game/old.jar" to null, "game/new.jar" to "/stage/new.jar".toPath()),
            mapOf("game/old.jar" to original, "game/new.jar" to null))
        assertFalse(fs.exists(root / "game/old.jar"))
        assertTrue(transaction.canRollback())
        transaction.rollback()
        assertEquals(original, fs.sha256(root / "game/old.jar"))
        assertFalse(fs.exists(root / "game/new.jar"))
        assertContentEquals(byteArrayOf(9), fs.read(root / "game/world.dat") { readByteArray() })
    }

    @Test fun rollsBackPartialWriteFailure() {
        val storage = FakeFileSystem()
        val root = "/instance".toPath()
        storage.createDirectories(root)
        storage.createDirectories("/stage".toPath())
        storage.write(root / "first") { writeByte(1) }
        storage.write(root / "second") { writeByte(2) }
        storage.write("/stage/new".toPath()) { writeByte(3) }
        var fail = true
        val fs = object : ForwardingFileSystem(storage) {
            override fun atomicMove(source: Path, target: Path) {
                if (target == root / "second" && fail) { fail = false; throw IOException("Injected disk error") }
                super.atomicMove(source, target)
            }
        }
        val transaction = ContentTransaction(fs, root, root / ".trestle/backup")
        assertFailsWith<IOException> {
            transaction.apply(mapOf("first" to "/stage/new".toPath(), "second" to "/stage/new".toPath()),
                mapOf("first" to fs.sha256(root / "first"), "second" to fs.sha256(root / "second")))
        }
        assertEquals(1, fs.read(root / "first") { readByte().toInt() })
        assertEquals(2, fs.read(root / "second") { readByte().toInt() })
        assertFalse(transaction.canRollback())
    }

    @Test fun refusesStalePreviewAndRollbackOverLaterEdits() {
        val fs = FakeFileSystem()
        val root = "/instance".toPath()
        fs.createDirectories(root)
        fs.write(root / "mod") { writeByte(1) }
        fs.write("/incoming".toPath()) { writeByte(2) }
        val transaction = ContentTransaction(fs, root, root / ".trestle/backup")
        assertFailsWith<IllegalArgumentException> { transaction.apply(mapOf("mod" to "/incoming".toPath()), mapOf("mod" to null)) }
        transaction.apply(mapOf("mod" to "/incoming".toPath()), mapOf("mod" to fs.sha256(root / "mod")))
        fs.write(root / "mod") { writeByte(3) }
        assertFailsWith<IllegalArgumentException> { transaction.rollback() }
        assertEquals(3, fs.read(root / "mod") { readByte().toInt() })
    }

    @Test fun rejectsTraversalAndSymlinkTargets() {
        val fs = FakeFileSystem().apply { emulateUnix() }
        val root = "/instance".toPath()
        fs.createDirectories(root)
        fs.createDirectories("/outside".toPath())
        fs.createSymlink(root / "mods", "/outside".toPath())
        listOf("../escape", "/absolute", "mods/escape", "C:/escape", "a\\b", "a/./b").forEach {
            assertFailsWith<IllegalArgumentException> { checkedContentPath(fs, root, it) }
        }
    }
}
