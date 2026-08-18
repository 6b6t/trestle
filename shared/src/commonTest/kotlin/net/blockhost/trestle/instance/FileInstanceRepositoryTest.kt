package net.blockhost.trestle.instance

import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FileInstanceRepositoryTest {
    @Test
    fun writesVersionCompatibleClientSettingsForNewInstance() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem,
            "/data/instances.json".toPath(),
            "/data/instances".toPath(),
            InstanceIdFactory { InstanceId("test01") },
        )
        repository.initialize()

        repository.create(
            CreateInstanceRequest(
                displayName = "Main",
                minecraftVersionId = "1.21.8",
                clientSettings = MinecraftClientSettings(
                    narratorMode = MinecraftNarratorMode.OFF,
                    masterVolumePercent = 45,
                    musicVolumePercent = 15,
                    fieldOfViewDegrees = 90,
                    brightnessPercent = 80,
                    mouseSensitivityPercent = 35,
                    maximumFrameRate = 160,
                    guiScale = 3,
                    renderDistanceChunks = 14,
                    simulationDistanceChunks = 8,
                    particles = MinecraftParticleSetting.MINIMAL,
                    autoJump = false,
                    showSubtitles = true,
                    enableVsync = true,
                    fullscreen = true,
                    viewBobbing = false,
                    invertMouse = true,
                    entityShadows = false,
                ),
            ),
        )

        val options = fileSystem.read("/data/instances/test01/game/options.txt".toPath()) { readUtf8() }
        assertEquals(
            """
            fullscreen:true
            enableVsync:true
            maxFps:160
            guiScale:3
            renderDistance:14
            particles:2
            bobView:false
            fov:0.5
            gamma:0.8
            mouseSensitivity:0.35
            invertYMouse:true
            soundCategory_master:0.45
            soundCategory_music:0.15
            entityShadows:false
            showSubtitles:true
            autoJump:false
            narrator:0
            simulationDistance:8
            """.trimIndent() + "\n",
            options,
        )
    }

    @Test
    fun updatesExistingClientSettingsWithoutRemovingOtherOptions() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem,
            "/data/instances.json".toPath(),
            "/data/instances".toPath(),
            InstanceIdFactory { InstanceId("test01") },
        )
        repository.initialize()
        val instance = repository.create(CreateInstanceRequest("Main", "1.21.8"))
        val optionsPath = "/data/instances/test01/game/options.txt".toPath()
        fileSystem.write(optionsPath) {
            writeUtf8(
                """
                renderDistance:18
                maxFps:60
                moddedOption:keep-me
                """.trimIndent() + "\n",
            )
        }

        val current = requireNotNull(repository.readClientSettings(instance.id))
        assertEquals(18, current.renderDistanceChunks)
        assertEquals(60, current.maximumFrameRate)

        repository.updateClientSettings(
            instance.id,
            current.copy(renderDistanceChunks = 24, maximumFrameRate = 144),
        )

        val options = fileSystem.read(optionsPath) { readUtf8() }
        assertTrue("renderDistance:24" in options)
        assertTrue("maxFps:144" in options)
        assertTrue("moddedOption:keep-me" in options)
        assertEquals(1, options.lineSequence().count { it.startsWith("renderDistance:") })
        assertEquals(emptyList(), fileSystem.list(optionsPath.parent!!).filter { it.name.endsWith(".tmp") })
    }

    @Test
    fun omitsClientSettingsUnknownToLegacyVersions() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem,
            "/data/instances.json".toPath(),
            "/data/instances".toPath(),
            InstanceIdFactory { InstanceId("legacy01") },
        )
        repository.initialize()

        repository.create(CreateInstanceRequest("Legacy", "1.5.2"))

        assertFalse(fileSystem.exists("/data/instances/legacy01/game/options.txt".toPath()))
    }

    @Test
    fun limitsClientSettingsToOptionsSupportedByTheSelectedVersion() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem,
            "/data/instances.json".toPath(),
            "/data/instances".toPath(),
            InstanceIdFactory { InstanceId("test1710") },
        )
        repository.initialize()

        repository.create(CreateInstanceRequest("Legacy modded", "1.7.10"))

        val options = fileSystem.read("/data/instances/test1710/game/options.txt".toPath()) { readUtf8() }
        assertTrue("renderDistance:12" in options)
        assertTrue("soundCategory_master:0.5" in options)
        assertFalse("narrator:" in options)
        assertFalse("simulationDistance:" in options)
    }

    @Test
    fun persistsRegistryAtomicallyAndReloadsIt() = runTest {
        val fileSystem = FakeFileSystem()
        val registry = "/data/instances.json".toPath()
        val instances = "/data/instances".toPath()
        val repository = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { InstanceId("test01") },
        )
        repository.initialize()
        val created = repository.create(CreateInstanceRequest("Main", "1.21.8"))
        repository.update(created.copy(installationState = InstallationState.Installed(1234)))

        val reloaded = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { InstanceId("test02") },
        )
        reloaded.initialize()

        assertEquals("Main", reloaded.instances.value.single().displayName)
        assertIs<InstallationState.Installed>(reloaded.instances.value.single().installationState)
        assertEquals(emptyList(), fileSystem.list("/data".toPath()).filter { it.name.endsWith(".tmp") })
    }

    @Test
    fun recoversPersistedInstallingStateAsInterrupted() = runTest {
        val fileSystem = FakeFileSystem()
        val registry = "/data/instances.json".toPath()
        val instances = "/data/instances".toPath()
        val repository = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { InstanceId("test01") },
        )
        repository.initialize()
        val created = repository.create(CreateInstanceRequest("Main", "1.21.8"))
        repository.update(
            created.copy(
                installationState = InstallationState.Installing(
                    completedBytes = 120,
                    totalBytes = 1_000,
                    completedFiles = 2,
                    totalFiles = 10,
                ),
            ),
        )

        val reloaded = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { InstanceId("test02") },
        )
        reloaded.initialize()

        val recovered = assertIs<InstallationState.Interrupted>(
            reloaded.instances.value.single().installationState,
        )
        assertEquals(120, recovered.completedBytes)
        assertEquals(2, recovered.completedFiles)

        val secondReload = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { InstanceId("test03") },
        )
        secondReload.initialize()
        assertIs<InstallationState.Interrupted>(secondReload.instances.value.single().installationState)
    }

    @Test
    fun keepsPinnedInstancesFirstAndRestoresRemovedEntries() = runTest {
        val fileSystem = FakeFileSystem()
        val registry = "/data/instances.json".toPath()
        val instances = "/data/instances".toPath()
        val ids = ArrayDeque(listOf(InstanceId("zebra01"), InstanceId("alpha01")))
        val repository = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { ids.removeFirst() },
        )
        repository.initialize()
        val zebra = repository.create(CreateInstanceRequest("Zebra", "1.21.8"))
        repository.create(CreateInstanceRequest("Alpha", "1.21.8"))

        val pinned = repository.update(zebra.copy(pinned = true))
        assertEquals(listOf("Zebra", "Alpha"), repository.instances.value.map { it.displayName })

        assertTrue(repository.delete(pinned.id))
        repository.restore(pinned)

        val reloaded = FileInstanceRepository(
            fileSystem,
            registry,
            instances,
            InstanceIdFactory { InstanceId("unused01") },
        )
        reloaded.initialize()
        assertTrue(reloaded.instances.value.first().pinned)
        assertEquals(pinned.id, reloaded.instances.value.first().id)
    }

    @Test
    fun clonesTheCompleteInstanceAndResetsUsageStatistics() = runTest {
        val fileSystem = FakeFileSystem()
        val ids = ArrayDeque(listOf(InstanceId("source01"), InstanceId("clone001")))
        val repository = FileInstanceRepository(
            fileSystem,
            "/data/instances.json".toPath(),
            "/data/instances".toPath(),
            InstanceIdFactory { ids.removeFirst() },
        )
        repository.initialize()
        val source = repository.create(CreateInstanceRequest("Source", "1.21.8"))
        fileSystem.createDirectories("/data/instances/source01/game/mods".toPath())
        fileSystem.write("/data/instances/source01/game/mods/example.jar".toPath()) { writeUtf8("mod") }
        repository.update(source.copy(launchCount = 4, playTimeMillis = 12_000, pinned = true))

        val clone = repository.clone(source.id, "Source Copy")

        assertEquals("Source Copy", clone.displayName)
        assertEquals(0, clone.launchCount)
        assertEquals(0, clone.playTimeMillis)
        assertFalse(clone.pinned)
        assertEquals(
            "mod",
            fileSystem.read("/data/instances/clone001/game/mods/example.jar".toPath()) { readUtf8() },
        )
    }

    @Test
    fun permanentlyDeletesTheInstanceDirectory() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem,
            "/data/instances.json".toPath(),
            "/data/instances".toPath(),
            InstanceIdFactory { InstanceId("delete01") },
        )
        repository.initialize()
        val instance = repository.create(CreateInstanceRequest("Delete", "1.21.8"))
        fileSystem.write("/data/instances/delete01/game/keep.txt".toPath()) { writeUtf8("no") }

        assertTrue(repository.deleteWithFiles(instance.id))

        assertTrue(repository.instances.value.isEmpty())
        assertFalse(fileSystem.exists("/data/instances/delete01".toPath()))
    }
}
