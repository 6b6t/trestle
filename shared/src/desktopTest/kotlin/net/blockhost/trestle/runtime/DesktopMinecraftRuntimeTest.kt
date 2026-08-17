package net.blockhost.trestle.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.blockhost.trestle.auth.NoSessionProvider
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.InstalledVersion
import net.blockhost.trestle.metadata.JavaVersionRequirement
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import net.blockhost.trestle.metadata.ResolvedLibrary
import net.blockhost.trestle.metadata.VersionMetadata
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
                javaVersion = JavaVersionRequirement("java-runtime-delta", 21),
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
        var resolvedComponent: String? = null
        var resolvedMajor = 0
        val runtime = DesktopMinecraftRuntime(
            environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64),
            directories = LauncherDirectories(root),
            sessionProvider = NoSessionProvider,
            installedVersionReader = { installed },
            javaResolver = JavaResolver { component, major ->
                resolvedComponent = component
                resolvedMajor = major
                "/managed/java-21/bin/java"
            },
        )

        val plan = runtime.prepare(instance)

        assertTrue(runtime.capabilities.supportsManagedJava)
        assertEquals("java-runtime-delta", resolvedComponent)
        assertEquals(21, resolvedMajor)
        assertEquals(listOf("Java account"), plan.missingRequirements)
        assertEquals(2, plan.classpathEntries.size)
        assertFalse(plan.safeCommand().joinToString(" ").contains("access-token"))
        assertEquals(2, plan.safeCommand().count { it == "<required:Java account>" })
    }

    @Test
    fun launchesProcessAndReportsSuccessfulExit() = runTest {
        val root = Files.createTempDirectory("trestle-launch-test")
        val runtime = DesktopMinecraftRuntime(
            environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64),
            directories = LauncherDirectories(root.toString().toPath()),
            sessionProvider = NoSessionProvider,
            installedVersionReader = { error("Not used by launch") },
            javaResolver = JavaResolver { _, _ -> error("Not used by launch") },
        )
        val launch = PreparedLaunch(
            instanceId = "test01",
            executable = javaExecutable(),
            arguments = listOf(CommandArgument.Public("-version")),
            workingDirectory = root.toString(),
            mainClass = "unused",
            classpathEntries = emptyList(),
            nativeDirectory = root.toString(),
        )

        val events = runtime.launch(launch).toList()

        assertTrue(events.first() is LaunchEvent.Started)
        assertEquals(0, (events.last() as LaunchEvent.Exited).exitCode)
    }

    @Test
    fun cancellationStopsAProcessWaitingWithoutOutput() = runTest {
        val root = Files.createTempDirectory("trestle-cancel-test")
        val source = root.resolve("WaitingProcess.java")
        Files.writeString(
            source,
            "class WaitingProcess { public static void main(String[] args) throws Exception { Thread.sleep(30000); } }",
        )
        val runtime = DesktopMinecraftRuntime(
            environment = PlatformEnvironment(OperatingSystem.LINUX, Architecture.X86_64),
            directories = LauncherDirectories(root.toString().toPath()),
            sessionProvider = NoSessionProvider,
            installedVersionReader = { error("Not used by launch") },
            javaResolver = JavaResolver { _, _ -> error("Not used by launch") },
        )
        val prepared = PreparedLaunch(
            instanceId = "test01",
            executable = javaExecutable(),
            arguments = listOf(CommandArgument.Public(source.toString())),
            workingDirectory = root.toString(),
            mainClass = "unused",
            classpathEntries = emptyList(),
            nativeDirectory = root.toString(),
        )
        val started = CompletableDeferred<Unit>()
        val job = launch {
            runtime.launch(prepared).collect { event ->
                if (event is LaunchEvent.Started) started.complete(Unit)
            }
        }

        started.await()
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                job.cancelAndJoin()
            }
        }
    }

    private fun javaExecutable(): String = Path.of(
        System.getProperty("java.home"),
        "bin",
        if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java",
    ).toString()
}
