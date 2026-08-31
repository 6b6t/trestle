package net.blockhost.trestle.resources

import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModpackOrigin
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.*

class ModpackUpdatesTest {
    @Test fun distinguishesUpstreamChangesFromLocalEditsAndProtectsWorlds() {
        val old = mapOf("mods/a.jar" to "a", "config/test.toml" to "b", "mods/remove.jar" to "c", "saves/world/level.dat" to "d")
        val incoming = mapOf("mods/a.jar" to "e", "config/test.toml" to "f", "mods/new.jar" to "g", "saves/world/level.dat" to "h")
        val current = old + mapOf("config/test.toml" to "custom", "mods/user.jar" to "user")
        val changes = planPackChanges(old, current, incoming)
        assertEquals(setOf("mods/a.jar", "config/test.toml", "mods/remove.jar", "mods/new.jar"), changes.map { it.path }.toSet())
        assertEquals(listOf("config/test.toml"), changes.filter { it.conflict }.map { it.path })
        assertEquals(1, changes.count { it.incomingHash == null })
    }

    @Test fun keepsModifiedConfigsAndUserModsAcrossUpdateAndRollback() {
        val fs = FakeFileSystem()
        fun instance(id: String, version: String) = GameInstance(InstanceId(id), id, version, instanceDirectory = "/$id",
            installationState = InstallationState.Installed(1), modpackOrigin = ModpackOrigin("MODRINTH", "pack", version, version, "Pack"))
        val original = instance("original", "1.0")
        val candidate = instance("candidate", "2.0")
        fun write(id: String, path: String, byte: Int) {
            val file = "/$id/$path".toPath()
            fs.createDirectories(file.parent!!)
            fs.write(file) { writeByte(byte) }
        }
        write("original", "game/mods/old.jar", 1)
        write("original", "game/config/mod.toml", 2)
        write("original", ".trestle/installed-version.json", 3)
        write("candidate", "game/mods/new.jar", 4)
        write("candidate", "game/config/mod.toml", 5)
        write("candidate", ".trestle/installed-version.json", 6)
        val updates = ModpackUpdates(fs)
        updates.record(original)
        updates.record(candidate)
        write("original", "game/config/mod.toml", 7)
        write("original", "game/mods/user.jar", 8)
        write("original", "game/saves/world/level.dat", 9)
        val preview = updates.preview(original, candidate)
        val updated = updates.apply(preview, emptySet())
        assertEquals(candidate.minecraftVersionId, updated.minecraftVersionId)
        assertFalse(fs.exists("/original/game/mods/old.jar".toPath()))
        assertEquals(7, fs.read("/original/game/config/mod.toml".toPath()) { readByte().toInt() })
        assertTrue(fs.exists("/original/game/mods/user.jar".toPath()))
        val restored = updates.rollback(updated)
        assertEquals(original.minecraftVersionId, restored.minecraftVersionId)
        assertFalse(fs.exists("/original/game/mods/new.jar".toPath()))
        assertTrue(fs.exists("/original/game/mods/old.jar".toPath()))
        assertEquals(9, fs.read("/original/game/saves/world/level.dat".toPath()) { readByte().toInt() })
    }
}
