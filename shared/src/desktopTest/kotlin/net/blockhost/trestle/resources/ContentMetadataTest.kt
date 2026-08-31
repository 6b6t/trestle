package net.blockhost.trestle.resources

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class ContentMetadataTest {
    @Test fun parsesEmbeddedNamesAuthorsAndDependencyIdentities() {
        val fs = FakeFileSystem()
        fs.write("/fabric.jar".toPath()) { write(archive("fabric.mod.json", """{"id":"sample","name":"Sample","version":"2.0","authors":["Alex",{"name":"Sam"}],"depends":{"fabricloader":">=0.16","minecraft":"1.21"}}""")) }
        val fabric = readEmbeddedMetadata(fs, "/fabric.jar".toPath())
        assertEquals(listOf("Alex", "Sam"), fabric.authors)
        assertEquals(setOf("fabricloader", "minecraft"), fabric.dependencies.toSet())
        fs.write("/forge.jar".toPath()) { write(archive("META-INF/mods.toml", """
            modLoader="javafml"
            [[mods]]
            modId="sample"
            displayName="Sample Forge"
            version="2.0"
            authors="Alex, Sam"
            [[dependencies.sample]]
            modId="minecraft"
            mandatory=true
        """.trimIndent())) }
        val forge = readEmbeddedMetadata(fs, "/forge.jar".toPath())
        assertEquals(fabric.authors, forge.authors)
        assertEquals(listOf("minecraft"), forge.dependencies)
    }

    @Test fun linksRenamedFilesByHashAndUsesCacheOffline() = runTest {
        val fs = FakeFileSystem()
        val root = "/instance".toPath()
        fs.createDirectories(root / "game/mods")
        val path = root / "game/mods/renamed.jar"
        fs.write(path) { write(archive("fabric.mod.json", """{"id":"sample","name":"Embedded","version":"1"}""")) }
        val hash = fs.sha1(path)
        var requests = 0
        val platform = ModrinthResourcePlatform(HttpClient(MockEngine { request ->
            requests++
            if (request.url.encodedPath.endsWith("/projects")) respond("""[{"id":"project","slug":"sample","title":"Catalog name","project_type":"mod"}]""")
            else respond("""{"id":"version","project_id":"project","name":"v1","version_number":"1","game_versions":["1.21"],"loaders":["fabric"],"version_type":"release","date_published":"2026-01-01","files":[{"hashes":{"sha1":"$hash"},"url":"https://example.test/a.jar","filename":"a.jar"}]}""")
        }), "test")
        val identifier = ContentIdentifier(ResourcePlatformRegistry(listOf(platform)), fs) { 10_000L }
        val instance = GameInstance(InstanceId("instance"), "Instance", "1.21", instanceDirectory = root.toString())
        val item = InstalledContent("local:mods/renamed.jar", "renamed.jar", ResourceType.MOD, listOf("renamed.jar"), true, true)
        val identified = identifier.identify(instance, listOf(item), true).single()
        assertTrue(identified.isTracked)
        assertEquals(hash, identified.contentSha1)
        assertEquals(2, requests)
        assertEquals(identified, identifier.identify(instance, listOf(item), false).single())
        assertEquals(2, requests)
        fs.write(path) { write(archive("fabric.mod.json", """{"id":"different","version":"2"}""")) }
        assertFalse(identifier.identify(instance, listOf(item), false).single().isTracked)
    }

    @Test fun fingerprintIgnoresOnlyCurseForgeWhitespace() {
        val fs = FakeFileSystem()
        fs.write("/first".toPath()) { write(byteArrayOf(1, 9, 2, 10, 3, 13, 4, 32, 5)) }
        fs.write("/second".toPath()) { write(byteArrayOf(1, 2, 3, 4, 5)) }
        fs.write("/third".toPath()) { write(byteArrayOf(1, 2, 3, 4, 5, 11)) }
        assertEquals(curseForgeFingerprint(fs, "/first".toPath()), curseForgeFingerprint(fs, "/second".toPath()))
        assertNotEquals(curseForgeFingerprint(fs, "/first".toPath()), curseForgeFingerprint(fs, "/third".toPath()))
    }

    private fun archive(name: String, body: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip -> zip.putNextEntry(ZipEntry(name)); zip.write(body.toByteArray()); zip.closeEntry() }
    }.toByteArray()
}
