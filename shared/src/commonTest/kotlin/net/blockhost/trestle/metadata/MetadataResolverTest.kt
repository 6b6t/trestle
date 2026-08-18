package net.blockhost.trestle.metadata

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataResolverTest {
    private val linux = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64, "6.8.0")

    @Test
    fun parsesOfficialVersionFields() {
        val metadata = Json.decodeFromString<VersionMetadata>(
            """
            {
              "id":"1.21.8",
              "type":"release",
              "mainClass":"net.minecraft.client.main.Main",
              "javaVersion":{"component":"java-runtime-delta","majorVersion":21},
              "downloads":{"client":{"sha1":"abc","size":42,"url":"https://piston-data.mojang.com/client.jar"}},
              "arguments":{"game":["--username","${'$'}{auth_player_name}"],"jvm":["-cp","${'$'}{classpath}"]}
            }
            """.trimIndent(),
        )

        assertEquals(21, metadata.javaVersion?.majorVersion)
        assertEquals("abc", metadata.downloads.client?.sha1)
        assertEquals(2, metadata.arguments?.game?.size)
    }

    @Test
    fun evaluatesOrderedAllowAndDisallowRules() {
        val rules = listOf(
            MojangRule(RuleAction.ALLOW),
            MojangRule(RuleAction.DISALLOW, os = RuleOs(name = "windows")),
        )
        assertTrue(MojangRuleEvaluator.allows(rules, linux))
        assertFalse(
            MojangRuleEvaluator.allows(
                rules,
                PlatformEnvironment(OperatingSystem.WINDOWS, Architecture.X86_64, "11"),
            ),
        )
    }

    @Test
    fun treatsMissingFeaturesAsFalse() {
        assertTrue(
            MojangRuleEvaluator.allows(
                listOf(MojangRule(RuleAction.ALLOW, features = mapOf("is_demo_user" to false))),
                linux,
            ),
        )
    }

    @Test
    fun resolvesMavenCoordinatePathWithClassifierAndExtension() {
        assertEquals(
            "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-linux.zip",
            MavenCoordinate.parse("org.lwjgl:lwjgl:3.3.3:natives-linux@zip").path(),
        )
    }

    @Test
    fun recognizesArchitectureAliases() {
        assertTrue("amd64" in Architecture.X86_64.aliases)
        assertTrue("aarch64" in Architecture.ARM64.aliases)
        assertTrue("arm64-v8a" in Architecture.ARM64.aliases)
        assertEquals("osx", OperatingSystem.MACOS.ruleName)
    }

    @Test
    fun resolvesConditionalArgumentObjects() {
        val values = buildJsonArray {
            add(buildJsonObject {
                put("rules", buildJsonArray {
                    add(buildJsonObject {
                        put("action", "allow")
                        put("os", buildJsonObject { put("name", "linux") })
                    })
                })
                put("value", buildJsonArray {
                    add("--width")
                    add("1280")
                })
            })
        }
        assertEquals(listOf("--width", "1280"), MojangArguments.resolve(values, linux))
    }

    @Test
    fun splitsQuotedLegacyArguments() {
        assertEquals(
            listOf("--username", "Player One", "--demo"),
            parseLegacyArguments("--username \"Player One\" --demo"),
        )
    }

    @Test
    fun loaderOverlayReplacesBaseLibraryAndKeepsModernJvmArguments() {
        val base = VersionMetadata(
            id = "1.21.1",
            mainClass = "net.minecraft.client.main.Main",
            downloads = VersionDownloads(client = DownloadReference(url = "https://cdn.test/client.jar")),
            libraries = listOf(MojangLibrary("example:library:1.0")),
            arguments = ModernArguments(
                game = buildJsonArray { add("--base-game") },
                jvm = buildJsonArray { add("--base-jvm") },
            ),
        )
        val overlay = VersionMetadata(
            id = "neoforge-21.1.1",
            mainClass = "io.example.Wrapper",
            libraries = listOf(MojangLibrary("example:library:2.0")),
            minecraftArguments = "--launchTarget neoforgeclient",
            inheritsFrom = "1.21.1",
        )

        val resolved = MinecraftMetadataResolver.resolve(MinecraftMetadataResolver.merge(base, overlay), linux)

        assertEquals(listOf("example:library:2.0"), resolved.libraries.map { it.name })
        assertEquals(listOf("--launchTarget", "neoforgeclient"), resolved.gameArguments)
        assertEquals(listOf("--base-jvm"), resolved.jvmArguments)
    }

    @Test
    fun marksGeneratedLoaderArtifactsAsOffClasspath() {
        val library = MojangLibrary(
            name = "example:generated:1.0",
            downloads = LibraryDownloads(
                artifact = DownloadReference(
                    url = "https://cdn.test/generated.jar",
                    path = "example/generated/1.0/generated-1.0.jar",
                ),
            ),
        )

        val resolved = MinecraftMetadataResolver.resolveLibraries(listOf(library), linux, classpath = false)

        assertFalse(resolved.single().classpath)
    }
}
