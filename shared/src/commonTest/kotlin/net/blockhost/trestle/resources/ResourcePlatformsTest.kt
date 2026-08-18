package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourcePlatformsTest {
    @Test
    fun searchesModrinthWithInstanceFacets() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Trestle test", request.headers[HttpHeaders.UserAgent])
            assertEquals("sodium", request.url.parameters["query"])
            val facets = requireNotNull(request.url.parameters["facets"])
            assertTrue(facets.contains("project_type:mod"))
            assertTrue(facets.contains("versions:1.21.8"))
            assertTrue(facets.contains("categories:fabric"))
            respond(
                """{"hits":[{"project_id":"AABBCCDD","slug":"sodium","title":"Sodium","description":"Renderer","author":"jellysquid","downloads":42,"icon_url":"https://cdn.test/icon.png","categories":["fabric","optimization"],"display_categories":["optimization"],"follows":12,"date_modified":"2026-08-01T12:00:00Z","license":"LGPL-3.0-only","client_side":"required","server_side":"unsupported","gallery":["https://cdn.test/gallery.png"],"featured_gallery":"https://cdn.test/featured.png"}],"offset":0,"total_hits":1}""",
                headers = jsonHeaders,
            )
        }

        val result = ModrinthResourcePlatform(HttpClient(engine), "Trestle test").search(
            ResourceSearchRequest("sodium", ResourceType.MOD, "1.21.8", ModLoader.FABRIC),
        )

        assertEquals("AABBCCDD", result.projects.single().id)
        assertEquals("https://modrinth.com/mod/sodium", result.projects.single().websiteUrl)
        assertEquals("https://cdn.test/icon.png", result.projects.single().iconUrl)
        assertEquals("https://cdn.test/featured.png", result.projects.single().featuredImageUrl)
        assertEquals(listOf("optimization"), result.projects.single().categories)
        assertEquals(ResourceEnvironmentSupport.REQUIRED, result.projects.single().clientSupport)
        assertEquals(ResourceEnvironmentSupport.UNSUPPORTED, result.projects.single().serverSupport)
        assertEquals("LGPL-3.0-only", result.projects.single().license)
        assertEquals(1, result.total)
    }

    @Test
    fun mapsModrinthVersionFilesAndDependencies() = runTest {
        val engine = MockEngine {
            respond(
                """[{"id":"version-1","project_id":"project-1","name":"Release","version_number":"1.0.0","game_versions":["1.21.8"],"loaders":["fabric"],"version_type":"release","date_published":"2026-01-01T00:00:00Z","files":[{"hashes":{"sha1":"abc"},"url":"https://cdn.test/mod.jar","filename":"mod.jar","primary":true,"size":42}],"dependencies":[{"project_id":"dependency","dependency_type":"required"}]}]""",
                headers = jsonHeaders,
            )
        }
        val project = project(ResourceProvider.MODRINTH)

        val version = ModrinthResourcePlatform(HttpClient(engine), "Trestle test")
            .versions(project, "1.21.8", ModLoader.FABRIC)
            .single()

        assertEquals("abc", version.primaryFile?.sha1)
        assertEquals(DependencyKind.REQUIRED, version.dependencies.single().kind)
        assertEquals("dependency", version.dependencies.single().projectId)
    }

    @Test
    fun searchesCurseForgeWithApplicationKeyAndNumericFilters() = runTest {
        val engine = MockEngine { request ->
            assertEquals("application-key", request.headers["x-api-key"])
            assertEquals("432", request.url.parameters["gameId"])
            assertEquals("6", request.url.parameters["classId"])
            assertEquals("4", request.url.parameters["modLoaderType"])
            respond(
                """{"data":[{"id":238222,"name":"Just Enough Items","slug":"jei","summary":"Recipe viewer","downloadCount":99,"authors":[{"name":"mezz"}],"links":{"websiteUrl":"https://curseforge.com/minecraft/mc-mods/jei","sourceUrl":"https://github.com/mezz/JustEnoughItems","issuesUrl":"https://github.com/mezz/JustEnoughItems/issues","wikiUrl":"https://github.com/mezz/JustEnoughItems/wiki"},"logo":{"thumbnailUrl":"https://cdn.test/logo.png"},"screenshots":[{"thumbnailUrl":"https://cdn.test/screenshot.png"}],"dateModified":"2026-08-02T12:00:00Z","categories":[{"name":"Map and Information"}]}],"pagination":{"index":0,"totalCount":1}}""",
                headers = jsonHeaders,
            )
        }

        val result = CurseForgeResourcePlatform(HttpClient(engine), "application-key").search(
            ResourceSearchRequest("jei", ResourceType.MOD, "1.21.8", ModLoader.FABRIC),
        )

        assertEquals("238222", result.projects.single().id)
        assertEquals("mezz", result.projects.single().author)
        assertEquals("https://cdn.test/logo.png", result.projects.single().iconUrl)
        assertEquals("https://cdn.test/screenshot.png", result.projects.single().featuredImageUrl)
        assertEquals("https://github.com/mezz/JustEnoughItems", result.projects.single().sourceUrl)
        assertEquals("https://github.com/mezz/JustEnoughItems/issues", result.projects.single().issuesUrl)
        assertEquals("https://github.com/mezz/JustEnoughItems/wiki", result.projects.single().wikiUrl)
    }

    @Test
    fun usesNativeNeoForgeFiltersForBothProviders() = runTest {
        val modrinthEngine = MockEngine { request ->
            assertTrue(requireNotNull(request.url.parameters["facets"]).contains("categories:neoforge"))
            respond("""{"hits":[],"offset":0,"total_hits":0}""", headers = jsonHeaders)
        }
        val curseForgeEngine = MockEngine { request ->
            assertEquals("6", request.url.parameters["modLoaderType"])
            respond("""{"data":[],"pagination":{"index":0,"totalCount":0}}""", headers = jsonHeaders)
        }
        val search = ResourceSearchRequest("", ResourceType.MOD, "1.21.1", ModLoader.NEOFORGE)

        ModrinthResourcePlatform(HttpClient(modrinthEngine), "Trestle test").search(search)
        CurseForgeResourcePlatform(HttpClient(curseForgeEngine), "application-key").search(search)
    }

    @Test
    fun preservesBlockedCurseForgeVersionForManualExplanation() = runTest {
        val engine = MockEngine {
            respond(
                """{"data":[{"id":7,"modId":123,"displayName":"Blocked","fileName":"blocked.jar","fileDate":"2026-01-01T00:00:00Z","releaseType":1,"downloadUrl":null,"fileLength":9,"hashes":[{"value":"def","algo":1}],"gameVersions":["1.21.8","Fabric"],"dependencies":[]}]}""",
                headers = jsonHeaders,
            )
        }

        val version = CurseForgeResourcePlatform(HttpClient(engine), "application-key")
            .versions(project(ResourceProvider.CURSEFORGE, id = "123"), "1.21.8", ModLoader.FABRIC)
            .single()

        assertNull(version.primaryFile?.url)
        assertEquals("def", version.primaryFile?.sha1)
        assertEquals(listOf("Fabric"), version.loaders)
    }

    @Test
    fun resolvesCurseForgeProjectClassesForModpackFiles() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v1/mods", request.url.encodedPath)
            respond(
                """{"data":[{"id":99,"classId":12,"name":"Pack resources","slug":"pack-resources","summary":"Textures","authors":[],"links":{},"categories":[]}]}""",
                headers = jsonHeaders,
            )
        }

        val project = CurseForgeResourcePlatform(HttpClient(engine), "application-key")
            .projectsByIds(listOf("99"))
            .getValue("99")

        assertEquals(ResourceType.RESOURCE_PACK, project.type)
    }

    @Test
    fun rejectsUnconfiguredCurseForgeBeforeNetworkAccess() = runTest {
        val platform = CurseForgeResourcePlatform(HttpClient(MockEngine { error("network should not run") }), "")

        val error = assertFailsWith<LauncherException.InvalidMetadata> {
            platform.search(ResourceSearchRequest(type = ResourceType.MOD))
        }

        assertTrue(error.message.orEmpty().contains("TRESTLE_CURSEFORGE_API_KEY"))
    }

    @Test
    fun includesProviderErrorDescription() = runTest {
        val engine = MockEngine {
            respond(
                """{"error":"invalid_input","description":"Unknown Minecraft version"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders,
            )
        }

        val error = assertFailsWith<LauncherException.Network> {
            ModrinthResourcePlatform(HttpClient(engine), "Trestle test")
                .search(ResourceSearchRequest(type = ResourceType.MOD))
        }

        assertTrue(error.message.orEmpty().contains("Unknown Minecraft version"))
    }

    @Test
    fun mapsFtbCatalogAndDirectInstallPlan() = runTest {
        val engine = MockEngine { request ->
            val body = when (request.url.encodedPath) {
                "/v1/modpacks/public/modpack/all" -> """{"packs":[42]}"""
                "/v1/modpacks/public/modpack/42" -> """{"id":42,"name":"Stone Pack","slug":"stone-pack","synopsis":"A pack","description":"Long description","installs":123,"updated":99,"authors":[{"name":"FTB Team"}],"art":[{"url":"https://cdn.test/icon.webp","type":"square"}],"tags":[{"name":"Questing"}],"versions":[{"id":7,"name":"1.0.0","type":"release","released":12,"targets":[{"name":"minecraft","version":"1.21.1","type":"game"},{"name":"neoforge","version":"21.1.1","type":"modloader"}]}]}"""
                "/v1/modpacks/public/modpack/42/7" -> """{"id":7,"name":"1.0.0","type":"release","released":12,"targets":[{"name":"minecraft","version":"1.21.1","type":"game"},{"name":"neoforge","version":"21.1.1","type":"modloader"}],"files":[{"path":"./mods","name":"example.jar","url":"https://cdn.test/example.jar","hashes":{"sha1":"abc"},"size":4}]}"""
                else -> error("Unexpected request: ${request.url}")
            }
            respond(body, headers = jsonHeaders)
        }
        val platform = FtbResourcePlatform(HttpClient(engine))
        val project = platform.search(ResourceSearchRequest(type = ResourceType.MODPACK)).projects.single()
        val summaryVersion = platform.versions(project, null, null).single()
        val resolvedVersion = platform.version(project.id, summaryVersion.id)

        assertEquals("Stone Pack", project.name)
        assertEquals(ModLoader.NEOFORGE, resolvedVersion.externalPack?.loader)
        assertEquals("mods/example.jar", resolvedVersion.externalPack?.files?.single()?.path)
        assertEquals("abc", resolvedVersion.externalPack?.files?.single()?.sha1)
    }

    @Test
    fun resolvesAtLauncherCatalogAndPackManifest() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Trestle test", request.headers[HttpHeaders.UserAgent])
            val body = when (request.url.encodedPath) {
                "/v1/packs/full/public" ->
                    """{"error":false,"data":[{"name":"Example Pack","safeName":"ExamplePack","description":"A public pack","supportURL":"https://support.test","websiteURL":"https://example.test","versions":[{"version":"1.0.0","minecraft":"1.21.1","published":42}]}]}"""
                "/packs/ExamplePack/versions/1.0.0/Configs.json" ->
                    """{"version":"1.0.0","minecraft":"1.21.1","loader":{"type":"fabric","metadata":{"loader":"0.16.14"}},"noConfigs":false,"configs":{"filesize":12,"sha1":"config-sha1"},"mods":[{"name":"Example Mod","version":"2.0","url":"packs/ExamplePack/files/example.jar","file":"example.jar","download":"server","type":"mods","md5":"mod-md5","filesize":7,"client":true}]}"""
                else -> error("Unexpected request: ${request.url}")
            }
            respond(body, headers = jsonHeaders)
        }
        val platform = AtLauncherResourcePlatform(
            httpClient = HttpClient(engine),
            userAgent = "Trestle test",
            apiBaseUrl = "https://api.test/v1",
            downloadBaseUrl = "https://cdn.test",
        )

        val project = platform.search(ResourceSearchRequest(type = ResourceType.MODPACK)).projects.single()
        val summaryVersion = platform.versions(project, null, null).single()
        val resolvedVersion = platform.version(project.id, summaryVersion.id)
        val plan = requireNotNull(resolvedVersion.externalPack)

        assertEquals("ExamplePack", project.id)
        assertEquals("https://cdn.test/launcher/images/examplepack.png", project.iconUrl)
        assertEquals(ModLoader.FABRIC, plan.loader)
        assertEquals("0.16.14", plan.loaderVersion)
        assertEquals("mods/example.jar", plan.files.single().path)
        assertEquals("https://cdn.test/packs/ExamplePack/files/example.jar", plan.files.single().url)
        assertEquals("mod-md5", plan.files.single().md5)
        assertEquals("https://cdn.test/packs/ExamplePack/versions/1.0.0/Configs.zip", plan.componentArchives.single().url)
        assertEquals("config-sha1", plan.componentArchives.single().sha1)
    }

    @Test
    fun resolvesTechnicSolderBuildIntoComponentArchives() = runTest {
        val engine = MockEngine { request ->
            val body = when (request.url.encodedPath) {
                "/modpack/example" -> """{"id":1,"name":"example","displayName":"Example Pack","user":"author","minecraft":"1.20.1","version":"2.0","description":"Pack description","solder":"https://solder.test/api/"}"""
                "/api/modpack/example" -> """{"recommended":"2.0","latest":"2.0","builds":["1.0","2.0"]}"""
                "/api/modpack/example/2.0" -> """{"minecraft":"1.20.1","forge":"47.3.0","mods":[{"name":"core","url":"https://cdn.test/core.zip","filesize":42}]}"""
                else -> error("Unexpected request: ${request.url}")
            }
            respond(body, headers = jsonHeaders)
        }
        val platform = TechnicResourcePlatform(HttpClient(engine))
        val project = ResourceProject(
            ResourceProvider.TECHNIC,
            "example",
            "example",
            "Example Pack",
            "",
            "author",
            ResourceType.MODPACK,
            0,
            null,
            null,
            emptyList(),
        )
        val selected = platform.versions(project, null, null).first()
        val resolved = platform.version(project.id, selected.id)

        assertEquals("2.0", selected.id)
        assertEquals(ModLoader.FORGE, resolved.externalPack?.loader)
        assertEquals("https://cdn.test/core.zip", resolved.externalPack?.componentArchives?.single()?.url)
    }

    private fun project(provider: ResourceProvider, id: String = "project-1") = ResourceProject(
        provider = provider,
        id = id,
        slug = "project",
        name = "Project",
        summary = "Summary",
        author = "Author",
        type = ResourceType.MOD,
        downloads = 1,
        iconUrl = null,
        websiteUrl = null,
        categories = emptyList(),
    )

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
