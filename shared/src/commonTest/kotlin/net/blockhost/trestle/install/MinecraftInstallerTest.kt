package net.blockhost.trestle.install

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.instance.CreateInstanceRequest
import net.blockhost.trestle.instance.FileInstanceRepository
import net.blockhost.trestle.instance.InstanceIdFactory
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.FabricMetadataClient
import net.blockhost.trestle.metadata.MinecraftMetadataClient
import net.blockhost.trestle.metadata.NeoForgeMetadataClient
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import okio.Path.Companion.toPath
import okio.ByteString.Companion.encodeUtf8
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MinecraftInstallerTest {
    @Test
    fun installsNeoForgeLaunchAndGeneratedLibraries() = runTest {
        val fileSystem = FakeFileSystem()
        val repository = FileInstanceRepository(
            fileSystem = fileSystem,
            registryPath = "/data/instances.json".toPath(),
            instancesDirectory = "/data/instances".toPath(),
            idFactory = InstanceIdFactory { InstanceId("neoforge-test") },
        )
        repository.initialize()
        val instance = repository.create(
            CreateInstanceRequest("NeoForge", "1.21.1", ModLoader.NEOFORGE),
        )
        val profile =
            """{"version":"21.1.1","mainClass":"io.example.Wrapper","libraries":[{"name":"example:launch:1.0","downloads":{"artifact":{"url":"https://cdn.test/launch.jar"}}}],"mavenFiles":[{"name":"example:generated:1.0","downloads":{"artifact":{"url":"https://cdn.test/generated.jar"}}}],"minecraftArguments":"--fml.neoForgeVersion 21.1.1 --launchTarget neoforgeclient","requires":[{"uid":"net.minecraft","equals":"1.21.1"}]}"""
        val profileSha256 = profile.encodeUtf8().sha256().hex()
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/mc/game/version_manifest_v2.json" -> respond(
                    """{"latest":{"release":"1.21.1","snapshot":"1.21.1"},"versions":[{"id":"1.21.1","type":"release","url":"https://meta.test/base.json"}]}""",
                    headers = jsonHeaders,
                )
                "/base.json" -> respond(
                    """{"id":"1.21.1","mainClass":"net.minecraft.client.main.Main","downloads":{"client":{"url":"https://cdn.test/client.jar"}},"arguments":{"game":["--base"],"jvm":["-cp","${'$'}{classpath}"]},"javaVersion":{"majorVersion":21}}""",
                    headers = jsonHeaders,
                )
                "/v1/net.neoforged/index.json" -> respond(
                    """{"versions":[{"version":"21.1.1","releaseTime":"2026-01-01T00:00:00Z","sha256":"$profileSha256","requires":[{"uid":"net.minecraft","equals":"1.21.1"}]}]}""",
                    headers = jsonHeaders,
                )
                "/v1/net.neoforged/21.1.1.json" -> respond(profile, headers = jsonHeaders)
                else -> respond("artifact")
            }
        })
        val installer = MinecraftInstaller(
            repository = repository,
            metadataClient = MinecraftMetadataClient(client),
            fabricMetadataClient = FabricMetadataClient(client),
            neoForgeMetadataClient = NeoForgeMetadataClient(client, "Trestle test"),
            downloadPipeline = DownloadPipeline(client, fileSystem),
            fileSystem = fileSystem,
            directories = LauncherDirectories("/data".toPath()),
            environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64),
            clock = EpochClock { 1L },
        )

        val installedInstance = installer.install(instance)
        val installedVersion = installer.readInstalledVersion(installedInstance)

        assertIs<InstallationState.Installed>(installedInstance.installationState)
        assertEquals("21.1.1", installedInstance.loaderVersion)
        assertEquals("io.example.Wrapper", installedVersion.metadata.mainClass)
        assertTrue(installedVersion.libraries.single { it.name == "example:launch:1.0" }.classpath)
        assertFalse(installedVersion.libraries.single { it.name == "example:generated:1.0" }.classpath)
        assertTrue(fileSystem.exists("/data/libraries/example/generated/1.0/generated-1.0.jar".toPath()))
    }

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
            neoForgeMetadataClient = NeoForgeMetadataClient(client, "Trestle test"),
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

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
