package net.blockhost.trestle.runtime

import kotlinx.coroutines.test.runTest
import net.blockhost.trestle.auth.NoSessionProvider
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.InstalledVersion
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.metadata.ResolvedLibrary
import net.blockhost.trestle.metadata.VersionMetadata
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopMinecraftRuntimeTest {
    @Test
    fun preparesSafeLaunchPlanWithoutCredentials() = runTest {
        val root = Files.createTempDirectory("trestle-runtime-test").toString().toPath()
        val instance = GameInstance(
            id = InstanceId("test01"),
            displayName = "Test",
            minecraftVersionId = "1.21.8",
            instanceDirectory = (root / "instances" / "test01").toString(),
            requiredJavaMajor = 21,
            installationState = InstallationState.Installed(1),
        )
        val installed = InstalledVersion(
            metadata = VersionMetadata(
                id = "1.21.8",
                mainClass = "net.minecraft.client.main.Main",
            ),
            libraries = listOf(
                ResolvedLibrary(
                    name = "example:library:1.0",
                    path = "example/library/1.0/library-1.0.jar",
                    url = "https://example.test/library.jar",
                    sha1 = null,
                    size = null,
                    native = false,
                ),
            ),
            requiredJavaMajor = 21,
            gameArguments = listOf("--username", "${'$'}{auth_player_name}", "--accessToken", "${'$'}{auth_access_token}"),
            jvmArguments = listOf("-Djava.library.path=${'$'}{natives_directory}", "-cp", "${'$'}{classpath}"),
        )
        val runtime = DesktopMinecraftRuntime(
            environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64),
            directories = LauncherDirectories(root),
            sessionProvider = NoSessionProvider,
            installedVersionReader = { installed },
            javaResolver = JavaResolver { "/managed/java-21/bin/java" },
        )

        val plan = runtime.prepare(instance)

        assertEquals(listOf("Java account"), plan.missingRequirements)
        assertEquals(2, plan.classpathEntries.size)
        assertFalse(plan.safeCommand().joinToString(" ").contains("access-token"))
        assertEquals(2, plan.safeCommand().count { it == "<required:Java account>" })
    }
}
