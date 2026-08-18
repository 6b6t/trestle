package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.download.DownloadPipeline
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceInstallerTest {
    @Test
    fun installsLocalContentInTheMatchingGameFolder() = runTest {
        val root = "/instances/local-files".toPath()
        val fileSystem = FakeFileSystem().apply { createDirectories(root / "game") }
        val installer = ResourceInstaller(
            ResourcePlatformRegistry(emptyList()),
            DownloadPipeline(HttpClient(MockEngine { respond("unused") }), fileSystem),
            fileSystem,
        )
        val bytes = byteArrayOf(1, 3, 3, 7)

        installer.installLocal(instance(root), "sodium.jar", bytes, ResourceType.MOD)
        installer.installLocal(instance(root), "faithful.zip", bytes, ResourceType.RESOURCE_PACK)
        installer.installLocal(instance(root), "complementary.zip", bytes, ResourceType.SHADER_PACK)

        assertContentEquals(bytes, fileSystem.read(root / "game" / "mods" / "sodium.jar") { readByteArray() })
        assertTrue(fileSystem.exists(root / "game" / "resourcepacks" / "faithful.zip"))
        assertTrue(fileSystem.exists(root / "game" / "shaderpacks" / "complementary.zip"))
        assertFailsWith<IllegalArgumentException> {
            installer.installLocal(instance(root), "sodium.jar", byteArrayOf(9), ResourceType.MOD)
        }
        assertContentEquals(bytes, fileSystem.read(root / "game" / "mods" / "sodium.jar") { readByteArray() })
    }

    @Test
    fun rejectsLocalContentWithTheWrongExtension() = runTest {
        val root = "/instances/local-files".toPath()
        val fileSystem = FakeFileSystem().apply { createDirectories(root / "game") }
        val installer = ResourceInstaller(
            ResourcePlatformRegistry(emptyList()),
            DownloadPipeline(HttpClient(MockEngine { respond("unused") }), fileSystem),
            fileSystem,
        )

        assertFailsWith<IllegalArgumentException> {
            installer.installLocal(instance(root), "not-a-mod.zip", byteArrayOf(1), ResourceType.MOD)
        }
        assertFalse(fileSystem.exists(root / "game" / "mods" / "not-a-mod.zip"))
    }

    @Test
    fun installsRequiredDependenciesAndRecordsOwnedFiles() = runTest {
        val root = "/instances/instance-1".toPath()
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(root / "game")
        val dependency = version(
            projectId = "dependency",
            id = "dependency-version",
            fileName = "dependency.jar",
            url = "https://cdn.test/dependency.jar",
        )
        val rootVersion = version(
            projectId = "root",
            id = "root-version",
            fileName = "root.jar",
            url = "https://cdn.test/root.jar",
            dependencies = listOf(
                ResourceDependency("dependency", null, null, DependencyKind.REQUIRED),
            ),
        )
        val platform = FakeResourcePlatform(ResourceProvider.MODRINTH, dependency)
        val client = HttpClient(MockEngine { request -> respond(request.url.encodedPath.encodeToByteArray()) })
        val installer = ResourceInstaller(
            ResourcePlatformRegistry(listOf(platform)),
            DownloadPipeline(client, fileSystem),
            fileSystem,
        )

        val summary = installer.install(instance(root), project("root"), rootVersion)

        assertEquals(1, summary.dependencyCount)
        assertTrue(fileSystem.exists(root / "game" / "mods" / "root.jar"))
        assertTrue(fileSystem.exists(root / "game" / "mods" / "dependency.jar"))
        assertTrue(installer.uninstall(instance(root), ResourceProvider.MODRINTH, "root"))
        assertFalse(fileSystem.exists(root / "game" / "mods" / "root.jar"))
        assertFalse(fileSystem.exists(root / "game" / "mods" / "dependency.jar"))
    }

    @Test
    fun usesHashMatchedModrinthFileForBlockedCurseForgeDownload() = runTest {
        val root = "/instances/instance-1".toPath()
        val fileSystem = FakeFileSystem().apply { createDirectories(root / "game") }
        val blocked = version(
            projectId = "curse-project",
            id = "curse-version",
            fileName = "blocked.jar",
            url = null,
            provider = ResourceProvider.CURSEFORGE,
            sha1 = "816d63e63c53fbbcdc73b605731fb850111597ab",
        )
        val alternative = version(
            projectId = "modrinth-project",
            id = "modrinth-version",
            fileName = "alternative.jar",
            url = "https://cdn.test/alternative.jar",
            sha1 = "816d63e63c53fbbcdc73b605731fb850111597ab",
        )
        val platforms = ResourcePlatformRegistry(
            listOf(
                FakeResourcePlatform(ResourceProvider.CURSEFORGE, blocked),
                FakeResourcePlatform(ResourceProvider.MODRINTH, alternative, alternative),
            ),
        )
        val client = HttpClient(MockEngine { respond("alternative") })
        val installer = ResourceInstaller(platforms, DownloadPipeline(client, fileSystem), fileSystem)

        installer.install(
            instance(root),
            project("curse-project", ResourceProvider.CURSEFORGE),
            blocked,
        )

        assertTrue(fileSystem.exists(root / "game" / "mods" / "alternative.jar"))
    }

    @Test
    fun keepsDependencyUntilAllDirectOwnersAreRemoved() = runTest {
        val root = "/instances/instance-1".toPath()
        val fileSystem = FakeFileSystem().apply { createDirectories(root / "game") }
        val dependency = version(
            projectId = "shared",
            id = "shared-version",
            fileName = "shared.jar",
            url = "https://cdn.test/shared.jar",
        )
        val required = listOf(ResourceDependency("shared", null, null, DependencyKind.REQUIRED))
        val first = version("first", "first-version", "first.jar", "https://cdn.test/first.jar", dependencies = required)
        val second = version("second", "second-version", "second.jar", "https://cdn.test/second.jar", dependencies = required)
        val platform = FakeResourcePlatform(ResourceProvider.MODRINTH, dependency)
        val installer = ResourceInstaller(
            ResourcePlatformRegistry(listOf(platform)),
            DownloadPipeline(HttpClient(MockEngine { request -> respond(request.url.encodedPath.encodeToByteArray()) }), fileSystem),
            fileSystem,
        )
        val instance = instance(root)

        installer.install(instance, project("first"), first)
        installer.install(instance, project("second"), second)
        installer.uninstall(instance, ResourceProvider.MODRINTH, "first")

        assertTrue(fileSystem.exists(root / "game" / "mods" / "shared.jar"))
        installer.uninstall(instance, ResourceProvider.MODRINTH, "second")
        assertFalse(fileSystem.exists(root / "game" / "mods" / "shared.jar"))
    }

    @Test
    fun rejectsDependencyUpdateUsedByAnotherResource() = runTest {
        val root = "/instances/instance-1".toPath()
        val fileSystem = FakeFileSystem().apply { createDirectories(root / "game") }
        val firstDependency = version("shared", "shared-v1", "shared-v1.jar", "https://cdn.test/shared-v1.jar")
        val secondDependency = version("shared", "shared-v2", "shared-v2.jar", "https://cdn.test/shared-v2.jar")
        val firstRoot = version(
            "first",
            "first-version",
            "first.jar",
            "https://cdn.test/first.jar",
            dependencies = listOf(ResourceDependency("shared", null, null, DependencyKind.REQUIRED)),
        )
        val secondRoot = version(
            "second",
            "second-version",
            "second.jar",
            "https://cdn.test/second.jar",
            dependencies = listOf(ResourceDependency("shared", "shared-v2", null, DependencyKind.REQUIRED)),
        )
        val client = HttpClient(MockEngine { request -> respond(request.url.encodedPath.encodeToByteArray()) })
        val firstInstaller = ResourceInstaller(
            ResourcePlatformRegistry(listOf(FakeResourcePlatform(ResourceProvider.MODRINTH, firstDependency))),
            DownloadPipeline(client, fileSystem),
            fileSystem,
        )
        firstInstaller.install(instance(root), project("first"), firstRoot)
        val secondInstaller = ResourceInstaller(
            ResourcePlatformRegistry(
                listOf(FakeResourcePlatform(ResourceProvider.MODRINTH, firstDependency, exactVersion = secondDependency)),
            ),
            DownloadPipeline(client, fileSystem),
            fileSystem,
        )

        assertFailsWith<LauncherException.InvalidMetadata> {
            secondInstaller.install(instance(root), project("second"), secondRoot)
        }
        assertTrue(fileSystem.exists(root / "game" / "mods" / "shared-v1.jar"))
        assertFalse(fileSystem.exists(root / "game" / "mods" / "shared-v2.jar"))
    }

    private fun instance(root: okio.Path) = GameInstance(
        id = InstanceId("instance-1"),
        displayName = "Instance",
        minecraftVersionId = "1.21.8",
        modLoader = ModLoader.FABRIC,
        loaderVersion = "0.16.0",
        instanceDirectory = root.toString(),
        installationState = InstallationState.Installed(1),
    )

    private fun project(id: String, provider: ResourceProvider = ResourceProvider.MODRINTH) = ResourceProject(
        provider = provider,
        id = id,
        slug = id,
        name = id,
        summary = "",
        author = "",
        type = ResourceType.MOD,
        downloads = 0,
        iconUrl = null,
        websiteUrl = null,
        categories = emptyList(),
    )

    private fun version(
        projectId: String,
        id: String,
        fileName: String,
        url: String?,
        provider: ResourceProvider = ResourceProvider.MODRINTH,
        sha1: String? = null,
        dependencies: List<ResourceDependency> = emptyList(),
    ) = ResourceVersion(
        provider = provider,
        id = id,
        projectId = projectId,
        name = id,
        versionNumber = "1.0.0",
        gameVersions = listOf("1.21.8"),
        loaders = listOf("fabric"),
        channel = ReleaseChannel.RELEASE,
        publishedAt = "2026-01-01T00:00:00Z",
        files = listOf(ResourceFile(null, fileName, url, sha1, null, true)),
        dependencies = dependencies,
    )
}

private class FakeResourcePlatform(
    override val provider: ResourceProvider,
    private val compatibleVersion: ResourceVersion,
    private val sha1Version: ResourceVersion? = null,
    private val exactVersion: ResourceVersion? = null,
) : ResourcePlatform {
    override val isAvailable = true

    override fun supports(type: ResourceType) = true

    override suspend fun search(request: ResourceSearchRequest) = ResourceSearchResult(emptyList(), 0, 0)

    override suspend fun versions(
        project: ResourceProject,
        gameVersion: String?,
        loader: ModLoader?,
    ) = listOf(compatibleVersion)

    override suspend fun version(projectId: String, versionId: String) = exactVersion ?: compatibleVersion

    override suspend fun versionBySha1(sha1: String) = sha1Version
}
