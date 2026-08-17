package net.blockhost.trestle.instance

import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FileInstanceRepositoryTest {
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
}
