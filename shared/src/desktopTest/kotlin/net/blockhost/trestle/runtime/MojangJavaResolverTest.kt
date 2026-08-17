package net.blockhost.trestle.runtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MojangJavaResolverTest {
    @Test
    fun provisionsVerifiedRuntimeAndReusesItOffline() = runTest {
        val root = Files.createTempDirectory("trestle-java-runtime-test").toString().toPath()
        val directories = LauncherDirectories(root)
        val executable = "#!/bin/sh\nexit 0\n"
        val executableSha1 = executable.encodeUtf8().sha1().hex()
        val manifest = """
            {
              "files": {
                "bin": {"type":"directory"},
                "bin/java": {
                  "downloads": {
                    "raw": {
                      "sha1":"$executableSha1",
                      "size":${executable.encodeUtf8().size},
                      "url":"https://runtime.test/java"
                    }
                  },
                  "executable":true,
                  "type":"file"
                }
              }
            }
        """.trimIndent()
        val manifestSha1 = manifest.encodeUtf8().sha1().hex()
        val index = """
            {
              "linux": {
                "java-runtime-delta": [{
                  "availability":{"group":1,"progress":100},
                  "manifest":{
                    "sha1":"$manifestSha1",
                    "size":${manifest.encodeUtf8().size},
                    "url":"https://runtime.test/manifest"
                  },
                  "version":{"name":"21.0.7","released":"2025-05-19T08:30:12+00:00"}
                }]
              }
            }
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/index" -> respond(index)
                "/manifest" -> respond(manifest)
                "/java" -> respond(executable)
                else -> error("Unexpected request: ${request.url}")
            }
        })
        val environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64)
        val resolver = MojangJavaResolver(
            environment = environment,
            directories = directories,
            httpClient = client,
            downloadPipeline = DownloadPipeline(client, FileSystem.SYSTEM, maxConcurrency = 1),
            runtimeIndexUrl = "https://runtime.test/index",
        )

        val resolved = resolver.resolve("java-runtime-delta", 21)

        assertTrue(Files.isExecutable(java.nio.file.Path.of(resolved)))
        assertEquals(executable, Files.readString(java.nio.file.Path.of(resolved)))

        val offlineClient = HttpClient(MockEngine { request -> error("Unexpected request: ${request.url}") })
        val offlineResolver = MojangJavaResolver(
            environment = environment,
            directories = directories,
            httpClient = offlineClient,
            downloadPipeline = DownloadPipeline(offlineClient, FileSystem.SYSTEM),
            runtimeIndexUrl = "https://runtime.test/index",
        )

        assertEquals(resolved, offlineResolver.resolve("java-runtime-delta", 21))
    }

    @Test
    fun mapsOnlyPlatformsPublishedByMojang() {
        assertEquals(
            "mac-os-arm64",
            mojangRuntimePlatform(PlatformEnvironment(OperatingSystem.MACOS, Architecture.ARM64)),
        )
        assertEquals(
            "windows-x64",
            mojangRuntimePlatform(PlatformEnvironment(OperatingSystem.WINDOWS, Architecture.X86_64)),
        )
        assertFailsWith<net.blockhost.trestle.domain.LauncherException.RuntimeUnavailable> {
            mojangRuntimePlatform(PlatformEnvironment(OperatingSystem.LINUX, Architecture.ARM64))
        }
    }
}
