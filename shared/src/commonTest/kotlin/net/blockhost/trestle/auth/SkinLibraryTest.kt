package net.blockhost.trestle.auth

import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SkinLibraryTest {
    @Test
    fun savesUpdatesAndReloadsSkinProfiles() = runTest {
        val fileSystem = FakeFileSystem()
        var time = 100L
        val library = SkinLibrary(fileSystem, "/data/skins".toPath()) { time++ }
        library.initialize()

        val original = minecraftSkinPng(height = 64, marker = 1)
        val saved = library.save("Midnight coat", SkinVariant.SLIM, original)
        val updatedTexture = minecraftSkinPng(height = 64, marker = 2)
        library.save("Midnight coat v2", SkinVariant.CLASSIC, updatedTexture, saved.profile.id)

        val reloaded = SkinLibrary(fileSystem, "/data/skins".toPath()) { time++ }
        reloaded.initialize()
        val profile = reloaded.skins.value.single()

        assertEquals(saved.profile.id, profile.profile.id)
        assertEquals("Midnight coat v2", profile.profile.name)
        assertEquals(SkinVariant.CLASSIC, profile.profile.variant)
        assertContentEquals(updatedTexture, profile.texture)
    }

    @Test
    fun deletesTextureAndRegistryEntry() = runTest {
        val fileSystem = FakeFileSystem()
        val library = SkinLibrary(fileSystem, "/data/skins".toPath()) { 100L }
        library.initialize()
        val saved = library.save("Builder", SkinVariant.CLASSIC, minecraftSkinPng())

        library.delete(saved.profile.id)

        assertTrue(library.skins.value.isEmpty())
        assertTrue(!fileSystem.exists("/data/skins/${saved.profile.textureFile}".toPath()))
    }

    @Test
    fun rejectsImagesOutsideMinecraftSkinDimensions() {
        assertFailsWith<LauncherException.FileSystem> {
            inspectMinecraftSkin(minecraftSkinPng(width = 128, height = 128))
        }
    }

    @Test
    fun acceptsTheLegacySkinLayout() {
        assertEquals(32, inspectMinecraftSkin(minecraftSkinPng(height = 32)).height)
    }

    @Test
    fun keepsDuplicateImportsAsSeparateProfiles() = runTest {
        val library = SkinLibrary(FakeFileSystem(), "/data/skins".toPath()) { 100L }
        library.initialize()
        val texture = minecraftSkinPng()

        val first = library.save("First", SkinVariant.CLASSIC, texture)
        val second = library.save("Second", SkinVariant.CLASSIC, texture)

        assertEquals(2, library.skins.value.size)
        assertTrue(first.profile.id != second.profile.id)
    }
}

internal fun minecraftSkinPng(width: Int = 64, height: Int = 64, marker: Int = 0): ByteArray =
    byteArrayOf(
        0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
        0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
        (width ushr 24).toByte(), (width ushr 16).toByte(), (width ushr 8).toByte(), width.toByte(),
        (height ushr 24).toByte(), (height ushr 16).toByte(), (height ushr 8).toByte(), height.toByte(),
        marker.toByte(),
    )
