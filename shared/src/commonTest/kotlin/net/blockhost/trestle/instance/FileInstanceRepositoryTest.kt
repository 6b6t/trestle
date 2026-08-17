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
                    renderDistanceChunks = 14,
                    simulationDistanceChunks = 8,
                    autoJump = false,
                    showSubtitles = true,
                    enableVsync = true,
                ),
            ),
        )

        val options = fileSystem.read("/data/instances/test01/game/options.txt".toPath()) { readUtf8() }
        assertEquals(
            """
            enableVsync:true
            renderDistance:14
            soundCategory_master:0.45
            soundCategory_music:0.15
            showSubtitles:true
            autoJump:false
            narrator:0
            simulationDistance:8
            """.trimIndent() + "\n",
            options,
        )
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
}
