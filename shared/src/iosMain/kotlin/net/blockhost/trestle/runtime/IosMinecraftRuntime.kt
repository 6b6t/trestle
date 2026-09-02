package net.blockhost.trestle.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.auth.AuthSession
import net.blockhost.trestle.auth.SessionProvider
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.metadata.InstalledVersion
import okio.FileSystem
import okio.Path.Companion.toPath

data class IosRuntimeAvailability(
    val available: Boolean,
    val reason: String? = null,
    val supportedJavaMajors: Set<Int> = emptySet(),
)

data class IosRuntimeDescriptor(
    val javaHome: String,
    val nativeDirectory: String,
    val classpathEntries: List<String>,
)

data class IosJvmLaunchRequest(
    val arguments: List<String>,
    val workingDirectory: String,
    val environment: Map<String, String>,
)

interface IosJvmLaunchObserver {
    fun onStarted()
    fun onOutput(line: String)
    fun onExited(exitCode: Int)
    fun onFailed(message: String)
}

interface IosRuntimeBridge {
    val availability: IosRuntimeAvailability
    fun runtime(javaMajor: Int): IosRuntimeDescriptor?
    fun launch(request: IosJvmLaunchRequest, observer: IosJvmLaunchObserver)
    fun cancel()
}

class UnavailableIosRuntimeBridge(
    reason: String = DEFAULT_REASON,
) : IosRuntimeBridge {
    override val availability = IosRuntimeAvailability(available = false, reason = reason)
    override fun runtime(javaMajor: Int): IosRuntimeDescriptor? = null
    override fun launch(request: IosJvmLaunchRequest, observer: IosJvmLaunchObserver) {
        observer.onFailed(requireNotNull(availability.reason))
    }
    override fun cancel() = Unit

    private companion object {
        const val DEFAULT_REASON =
            "Game launch needs a JIT-enabled iOS runtime bridge with patched Java and graphics libraries."
    }
}

internal class IosMinecraftRuntime(
    private val bridge: IosRuntimeBridge,
    private val directories: LauncherDirectories,
    private val sessionProvider: SessionProvider,
    private val installedVersionReader: (GameInstance) -> InstalledVersion,
    private val logger: LauncherLogger,
) : MinecraftRuntime {
    override val capabilities = RuntimeCapabilities(
        canPrepareLaunch = bridge.availability.available,
        canLaunch = bridge.availability.available,
        supportsManagedJava = bridge.availability.available,
        supportsNativeExtraction = bridge.availability.available,
        unavailableReason = bridge.availability.reason,
    )

    override suspend fun prepare(
        instance: GameInstance,
        options: LaunchOptions,
        onProgress: suspend (RuntimePreparationProgress) -> Unit,
    ): PreparedLaunch {
        if (!bridge.availability.available) {
            throw LauncherException.RuntimeUnavailable(
                bridge.availability.reason ?: "The iOS runtime bridge is not available.",
            )
        }
        val installed = installedVersionReader(instance)
        val runtime = bridge.runtime(installed.requiredJavaMajor)
            ?: throw LauncherException.RuntimeUnavailable(
                "The iOS runtime bundle does not include Java ${installed.requiredJavaMajor}.",
            )
        val session = sessionProvider.currentSession(instance.accountProfileId)
        val gameDirectory = instance.instanceDirectory.toPath() / "game"
        FileSystem.SYSTEM.createDirectories(gameDirectory)
        val clientJar = directories.versions / instance.minecraftVersionId / "${instance.minecraftVersionId}.jar"
        val libraries = installed.libraries
            .filter { it.classpath && !it.native && !it.name.startsWith("org.lwjgl:") }
            .map { directories.libraries / it.path }
        val classpathEntries = (runtime.classpathEntries + libraries.map(Any::toString) + clientJar.toString()).distinct()
        val classpath = classpathEntries.joinToString(":")
        val values = launchValues(instance, installed, session, runtime.nativeDirectory, classpath)
        val jvmArguments = buildList {
            add(CommandArgument.Public("-Xms${instance.memory.minimumMiB}M"))
            add(CommandArgument.Public("-Xmx${instance.memory.maximumMiB}M"))
            installed.jvmArguments
                .filterNot(::isDesktopOnlyJvmArgument)
                .mapTo(this) { CommandArgument.Public(substitutePublic(it, values)) }
            add(CommandArgument.Public("-Djava.class.path=$classpath"))
            add(CommandArgument.Public("-Djava.library.path=${runtime.nativeDirectory}"))
            add(CommandArgument.Public("-Djava.home=${runtime.javaHome}"))
            add(CommandArgument.Public("-Duser.home=$gameDirectory"))
            add(CommandArgument.Public("-Duser.dir=$gameDirectory"))
            add(CommandArgument.Public("-Dos.name=iOS"))
            add(CommandArgument.Public("-Dorg.lwjgl.glfw.checkThread0=false"))
            add(CommandArgument.Public("-Dorg.lwjgl.system.allocator=system"))
            add(CommandArgument.Public("-Dfml.earlyprogresswindow=false"))
            addAll(JvmArgumentPolicy.review(instance.jvmArguments).accepted.map(CommandArgument::Public))
            addAll(JvmArgumentPolicy.review(options.additionalJvmArguments).accepted.map(CommandArgument::Public))
        }
        val gameArguments = buildList {
            (installed.gameArguments + instance.gameArguments + options.additionalGameArguments)
                .mapTo(this) { substituteArgument(it, values, session) }
            if (options.demo) add(CommandArgument.Public("--demo"))
        }
        val arguments = jvmArguments + CommandArgument.Public(installed.metadata.mainClass) + gameArguments
        return PreparedLaunch(
            instanceId = instance.id.value,
            executable = "${runtime.javaHome}/bin/java",
            arguments = arguments,
            workingDirectory = gameDirectory.toString(),
            environment = mapOf(
                "HOME" to directories.root.toString(),
                "JAVA_HOME" to runtime.javaHome,
                "POJAV_HOME" to directories.root.toString(),
                "POJAV_GAME_DIR" to gameDirectory.toString(),
                "TMPDIR" to (directories.root / "tmp").toString(),
            ) + instance.environmentVariables,
            mainClass = installed.metadata.mainClass,
            classpathEntries = classpathEntries,
            nativeDirectory = runtime.nativeDirectory,
            missingRequirements = if (session == null) listOf("Java account") else emptyList(),
            jvmArguments = jvmArguments,
            gameArguments = gameArguments,
        ).also {
            logger.info(
                "runtime",
                "Prepared iOS Minecraft launch",
                mapOf("instanceId" to instance.id.value, "javaMajor" to installed.requiredJavaMajor),
            )
        }
    }

    override fun launch(preparedLaunch: PreparedLaunch): Flow<LaunchEvent> = callbackFlow {
        if (preparedLaunch.missingRequirements.isNotEmpty()) {
            trySend(LaunchEvent.Failed("Sign in with a Minecraft: Java Edition account before launching."))
            close()
            return@callbackFlow
        }
        val arguments = listOf(preparedLaunch.executable) + preparedLaunch.processArguments()
        bridge.launch(
            IosJvmLaunchRequest(arguments, preparedLaunch.workingDirectory, preparedLaunch.environment),
            object : IosJvmLaunchObserver {
                override fun onStarted() {
                    trySend(LaunchEvent.Started(null))
                }

                override fun onOutput(line: String) {
                    trySend(LaunchEvent.Log(line.take(MAX_LOG_LINE)))
                }

                override fun onExited(exitCode: Int) {
                    trySend(LaunchEvent.Exited(exitCode))
                    close()
                }

                override fun onFailed(message: String) {
                    trySend(LaunchEvent.Failed(message))
                    close()
                }
            },
        )
        awaitClose(bridge::cancel)
    }

    private fun launchValues(
        instance: GameInstance,
        installed: InstalledVersion,
        session: AuthSession?,
        nativeDirectory: String,
        classpath: String,
    ): Map<String, String> = buildMap {
        putAll(
            mapOf(
                "natives_directory" to nativeDirectory,
                "launcher_name" to "Trestle",
                "launcher_version" to BuildInfo.VERSION,
                "classpath" to classpath,
                "classpath_separator" to ":",
                "library_directory" to directories.libraries.toString(),
                "version_name" to installed.metadata.id,
                "game_directory" to (instance.instanceDirectory.toPath() / "game").toString(),
                "assets_root" to directories.assets.toString(),
                "assets_index_name" to (installed.assetIndexId ?: installed.metadata.assets.orEmpty()),
                "version_type" to installed.metadata.type,
                "user_properties" to "{}",
            ),
        )
        session?.let {
            put("auth_player_name", it.playerName)
            put("auth_uuid", it.profileId)
            put("user_type", it.userType)
            put("clientid", it.clientId)
            put("auth_xuid", it.xuid)
        }
    }

    private fun isDesktopOnlyJvmArgument(argument: String): Boolean =
        argument == "-cp" ||
            argument == "-classpath" ||
            argument.contains("\${classpath}") ||
            argument.startsWith("-Djava.library.path=") ||
            argument.startsWith("-Dos.name=") ||
            argument.startsWith("-Dos.version=")

    private fun substitutePublic(argument: String, values: Map<String, String>): String =
        PLACEHOLDER.replace(argument) { match -> values[match.groupValues[1]] ?: match.value }

    private fun substituteArgument(
        argument: String,
        values: Map<String, String>,
        session: AuthSession?,
    ): CommandArgument {
        if (session == null && AUTH_PLACEHOLDERS.any(argument::contains)) {
            return CommandArgument.RequiredCredential("Java account")
        }
        if (argument == "\${auth_access_token}") {
            return requireNotNull(session).accessToken?.let(CommandArgument::Secret) ?: CommandArgument.Public("0")
        }
        return CommandArgument.Public(substitutePublic(argument, values))
    }

    private companion object {
        const val MAX_LOG_LINE = 8_000
        val PLACEHOLDER = Regex("\\$\\{([^}]+)\\}")
        val AUTH_PLACEHOLDERS = listOf(
            "\${auth_player_name}",
            "\${auth_uuid}",
            "\${auth_access_token}",
            "\${user_type}",
            "\${clientid}",
            "\${auth_xuid}",
        )
    }
}
