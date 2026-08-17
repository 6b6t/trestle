package net.blockhost.trestle.install

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.instance.FileInstanceRepository
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertIs

class MinecraftInstallerTest {
    @Test
    fun cancellationLeavesTheInstanceReadyToResume() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem = fileSystem,
            registryPath = "/data/instances.json".toPath(),
            instancesDirectory = "/data/instances".toPath(),
            idFactory = InstanceIdFactory { InstanceId("resume-test") },
        )
        repository.initialize()
        val instance = repository.create(CreateInstanceRequest("Test", "1.21.8"))
        val client = HttpClient(MockEngine {
            delay(Long.MAX_VALUE)
            respond("")
        })
        val installer = MinecraftInstaller(
            repository = repository,
            metadataClient = MinecraftMetadataClient(client),
            fabricMetadataClient = FabricMetadataClient(client),
            downloadPipeline = DownloadPipeline(client, fileSystem),
            fileSystem = fileSystem,
            directories = LauncherDirectories("/data".toPath()),
            environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64),
            clock = EpochClock { 1L },
        )

        val installation = async { installer.install(instance) }
        testScheduler.runCurrent()
        assertIs<InstallationState.Installing>(repository.get(instance.id)?.installationState)

        installation.cancelAndJoin()

        assertIs<InstallationState.Interrupted>(repository.get(instance.id)?.installationState)
    }
}
