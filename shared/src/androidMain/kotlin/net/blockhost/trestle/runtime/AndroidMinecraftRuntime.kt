package net.blockhost.trestle.runtime

import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.auth.AuthSession
import net.blockhost.trestle.auth.SessionProvider
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.download.DownloadPipeline
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.metadata.InstalledVersion
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class AndroidMinecraftRuntime internal constructor(
    context: Context,
    private val architecture: Architecture,
    private val graphicsCompatibility: AndroidGraphicsCompatibility,
    private val directories: LauncherDirectories,
    private val sessionProvider: SessionProvider,
    private val installedVersionReader: (GameInstance) -> InstalledVersion,
    downloadPipeline: DownloadPipeline,
    fileSystem: FileSystem,
    private val logger: LauncherLogger,
) : MinecraftRuntime {
    private val applicationContext = context.applicationContext
    private val javaRuntimeManager = AndroidJavaRuntimeManager(
        directories,
        downloadPipeline,
        fileSystem,
        logger,
    )
    private val componentManager = AndroidGameComponentManager(
        directories,
        downloadPipeline,
        fileSystem,
        logger,
    )

    private val supportsArchitecture = AndroidRuntimeAbi.entries.any { it.architecture == architecture }
    private val unavailableReason = when {
        !supportsArchitecture -> "Android game launch requires an ARM64 or x64 process."
        !graphicsCompatibility.isSupported -> graphicsCompatibility.unavailableReason
        else -> null
    }

    override val capabilities = RuntimeCapabilities(
        canPrepareLaunch = unavailableReason == null,
        canLaunch = unavailableReason == null,
        supportsManagedJava = supportsArchitecture,
        supportsNativeExtraction = supportsArchitecture,
        unavailableReason = unavailableReason,
        supportedMinecraftVersions = setOf(MVP_VERSION),
        supportedModLoaders = setOf(ModLoader.VANILLA),
    )

    init {
        logger.info(
            "runtime",
            "Inspected Android graphics compatibility",
            mapOf(
                "graphics" to graphicsCompatibility.summary(),
                "gpuFamily" to graphicsCompatibility.gpuFamily.label,
                "supported" to graphicsCompatibility.isSupported,
            ),
        )
    }

    override suspend fun prepare(
        instance: GameInstance,
        options: LaunchOptions,
        onProgress: suspend (RuntimePreparationProgress) -> Unit,
    ): PreparedLaunch =
        withContext(Dispatchers.IO) {
            requireSupportedInstance(instance)
            val installed = installedVersionReader(instance)
            if (installed.requiredJavaMajor != REQUIRED_JAVA_MAJOR) {
                throw LauncherException.RuntimeUnavailable(
                    "Minecraft $MVP_VERSION requires Java $REQUIRED_JAVA_MAJOR, but the installed manifest requests " +
                        "Java ${installed.requiredJavaMajor}.",
                )
            }
            val session = sessionProvider.currentSession(instance.accountProfileId)
                ?: return@withContext PreparedLaunch(
                    instanceId = instance.id.value,
                    executable = "",
                    arguments = emptyList(),
                    workingDirectory = (instance.instanceDirectory.toPath() / "game").toString(),
                    mainClass = installed.metadata.mainClass,
                    classpathEntries = emptyList(),
                    nativeDirectory = "",
                    missingRequirements = listOf("Java account"),
                )
            val java = javaRuntimeManager.resolve(installed.requiredJavaMajor, architecture) { progress ->
                onProgress(progress.toRuntimeProgress("Downloading Java 25 runtime"))
            }
            val components = componentManager.resolve(AndroidRuntimeAbi.forArchitecture(architecture)) { progress ->
                onProgress(progress.toRuntimeProgress("Downloading Android game components"))
            }
            val gameDirectory = instance.instanceDirectory.toPath() / "game"
            FileSystem.SYSTEM.createDirectories(gameDirectory)

            val clientJar = directories.versions / instance.minecraftVersionId /
                "${instance.minecraftVersionId}.jar"
            val gameLibraries = installed.libraries
                .filter { it.classpath && !it.native && !it.name.startsWith("org.lwjgl:") }
                .map { directories.libraries / it.path }
            val classpathEntries = (components.classpath + gameLibraries + clientJar)
                .distinct()
                .map(Any::toString)
            val classpath = classpathEntries.joinToString(ANDROID_CLASSPATH_SEPARATOR)
            val values = launchValues(
                instance,
                installed,
                session,
                components.nativeDirectory.toString(),
                classpath,
            )
            val jvmArguments = buildList {
                add(CommandArgument.Public("-Xms${instance.memory.minimumMiB}M"))
                add(CommandArgument.Public("-Xmx${instance.memory.maximumMiB}M"))
                installed.jvmArguments
                    .filterNot(::isDesktopOnlyJvmArgument)
                    .mapTo(this) { CommandArgument.Public(substitutePublic(it, values)) }
                add(CommandArgument.Public("-Djava.class.path=$classpath"))
                add(CommandArgument.Public("-Djava.library.path=${components.nativeDirectory}"))
                add(CommandArgument.Public("-Djava.home=${java.home}"))
                add(CommandArgument.Public("-Djava.io.tmpdir=${applicationContext.cacheDir.absolutePath}"))
                add(CommandArgument.Public("-Duser.home=$gameDirectory"))
                add(CommandArgument.Public("-Duser.language=${Locale.getDefault().language}"))
                add(CommandArgument.Public("-Duser.timezone=${TimeZone.getDefault().id}"))
                add(CommandArgument.Public("-Dos.name=Linux"))
                add(CommandArgument.Public("-Dos.version=Android-${android.os.Build.VERSION.RELEASE}"))
                add(CommandArgument.Public("-Djna.boot.library.path=${components.nativeDirectory}"))
                add(CommandArgument.Public("-Dorg.lwjgl.vulkan.libname=libvulkan.so"))
                add(CommandArgument.Public("-Dorg.lwjgl.opengl.libname=libglxshim.so"))
                add(CommandArgument.Public("-Dorg.lwjgl.freetype.libname=${components.nativeDirectory}/libfreetype.so"))
                add(CommandArgument.Public("-Dorg.lwjgl.spvc.libname=spirv-cross-c-shared"))
                add(CommandArgument.Public("-Dorg.lwjgl.system.allocator=system"))
                add(CommandArgument.Public("-Dglfwstub.initEgl=false"))
                add(CommandArgument.Public("-Djdk.lang.Process.launchMechanism=FORK"))
                add(CommandArgument.Public("-XX:ActiveProcessorCount=${Runtime.getRuntime().availableProcessors()}"))
                addAll(JvmArgumentPolicy.review(instance.jvmArguments).accepted.map(CommandArgument::Public))
                addAll(JvmArgumentPolicy.review(options.additionalJvmArguments).accepted.map(CommandArgument::Public))
                installed.loggingPath?.let { loggingPath ->
                    installed.metadata.logging["client"]?.let { configuration ->
                        add(
                            CommandArgument.Public(
                                configuration.argument.replace(
                                    "\${path}",
                                    (directories.logging / loggingPath).toString(),
                                ),
                            ),
                        )
                    }
                }
            }
            val gameArguments = buildList {
                (installed.gameArguments + instance.gameArguments + options.additionalGameArguments)
                    .mapTo(this) { substituteArgument(it, values, session) }
                if (options.demo) add(CommandArgument.Public("--demo"))
            }
            val allArguments = jvmArguments + CommandArgument.Public(installed.metadata.mainClass) + gameArguments

            PreparedLaunch(
                instanceId = instance.id.value,
                executable = java.home.toString(),
                arguments = allArguments,
                workingDirectory = gameDirectory.toString(),
                environment = androidEnvironment(java.home.toString(), components.nativeDirectory.toString()) +
                    instance.environmentVariables,
                mainClass = installed.metadata.mainClass,
                classpathEntries = classpathEntries,
                nativeDirectory = components.nativeDirectory.toString(),
                missingRequirements = emptyList(),
                jvmArguments = jvmArguments,
                gameArguments = gameArguments,
            ).also { prepared ->
                logger.info(
                    "runtime",
                    "Prepared Android Minecraft launch",
                    mapOf(
                        "instanceId" to instance.id.value,
                        "version" to instance.minecraftVersionId,
                        "classpathEntries" to prepared.classpathEntries.size,
                        "authenticated" to true,
                    ),
                )
            }
        }

    override fun launch(preparedLaunch: PreparedLaunch): Flow<LaunchEvent> = callbackFlow {
        if (preparedLaunch.missingRequirements.isNotEmpty()) {
            trySend(LaunchEvent.Failed("Sign in with a Minecraft: Java Edition account before launching."))
            close()
            return@callbackFlow
        }
        val launchId = UUID.randomUUID().toString()
        val launchStartedAt = System.currentTimeMillis()
        val terminalReceived = AtomicBoolean(false)
        val monitorStarted = AtomicBoolean(false)
        val processAnnounced = AtomicBoolean(false)
        fun monitorProcess(processId: Long) {
            processAnnounced.set(true)
            if (processId <= 0 || !monitorStarted.compareAndSet(false, true)) return
            this@callbackFlow.launch(Dispatchers.IO) {
                val processDirectory = File("/proc/$processId")
                while (isActive && processDirectory.exists()) delay(PROCESS_POLL_MILLIS)
                if (isActive && terminalReceived.compareAndSet(false, true)) {
                    delay(EXIT_DIAGNOSTICS_DELAY_MILLIS)
                    val message = unexpectedProcessDeath(
                        preparedLaunch,
                        launchId,
                        processId.toInt(),
                        launchStartedAt,
                    )
                    logger.error(
                        "runtime",
                        message,
                        details = mapOf("instanceId" to preparedLaunch.instanceId),
                    )
                    trySend(LaunchEvent.Failed(message))
                    close()
                }
            }
        }
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                when (resultCode) {
                    AndroidGameLaunchProtocol.RESULT_STARTED -> {
                        val processId = resultData
                            ?.takeIf { it.containsKey(AndroidGameLaunchProtocol.EXTRA_PROCESS_ID) }
                            ?.getLong(AndroidGameLaunchProtocol.EXTRA_PROCESS_ID)
                        trySend(LaunchEvent.Started(processId))
                        processId?.let(::monitorProcess)
                    }
                    AndroidGameLaunchProtocol.RESULT_PROCESS_CREATED -> {
                        resultData
                            ?.takeIf { it.containsKey(AndroidGameLaunchProtocol.EXTRA_PROCESS_ID) }
                            ?.getLong(AndroidGameLaunchProtocol.EXTRA_PROCESS_ID)
                            ?.let(::monitorProcess)
                    }
                    AndroidGameLaunchProtocol.RESULT_LOG -> {
                        resultData?.getString(AndroidGameLaunchProtocol.EXTRA_MESSAGE)?.let { line ->
                            logger.debug("minecraft", line, mapOf("instanceId" to preparedLaunch.instanceId))
                            trySend(LaunchEvent.Log(line))
                        }
                    }
                    AndroidGameLaunchProtocol.RESULT_EXITED -> {
                        if (!terminalReceived.compareAndSet(false, true)) return
                        val exitCode = resultData?.getInt(AndroidGameLaunchProtocol.EXTRA_EXIT_CODE, 1) ?: 1
                        trySend(LaunchEvent.Exited(exitCode))
                        close()
                    }
                    AndroidGameLaunchProtocol.RESULT_FAILED -> {
                        if (!terminalReceived.compareAndSet(false, true)) return
                        val message = resultData?.getString(AndroidGameLaunchProtocol.EXTRA_MESSAGE)
                            ?: "Minecraft could not start."
                        trySend(LaunchEvent.Failed(message))
                        close()
                    }
                }
            }
        }
        val intent = Intent().apply {
            setClassName(applicationContext, AndroidGameLaunchProtocol.GAME_ACTIVITY_CLASS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AndroidGameLaunchProtocol.EXTRA_LAUNCH_ID, launchId)
            putExtra(AndroidGameLaunchProtocol.EXTRA_RECEIVER, receiver)
            putExtra(AndroidGameLaunchProtocol.EXTRA_RUNTIME_HOME, preparedLaunch.executable)
            putExtra(AndroidGameLaunchProtocol.EXTRA_WORKING_DIRECTORY, preparedLaunch.workingDirectory)
            putExtra(AndroidGameLaunchProtocol.EXTRA_NATIVE_DIRECTORY, preparedLaunch.nativeDirectory)
            putStringArrayListExtra(
                AndroidGameLaunchProtocol.EXTRA_JVM_ARGUMENTS,
                ArrayList(preparedLaunch.jvmArguments.map { it.reveal() }),
            )
            putExtra(AndroidGameLaunchProtocol.EXTRA_MAIN_CLASS, preparedLaunch.mainClass)
            putStringArrayListExtra(
                AndroidGameLaunchProtocol.EXTRA_GAME_ARGUMENTS,
                ArrayList(preparedLaunch.gameArguments.map { it.reveal() }),
            )
            putStringArrayListExtra(
                AndroidGameLaunchProtocol.EXTRA_ENVIRONMENT,
                ArrayList(preparedLaunch.environment.flatMap { (key, value) -> listOf(key, value) }),
            )
        }
        try {
            applicationContext.startActivity(intent)
            launch {
                delay(PROCESS_START_TIMEOUT_MILLIS)
                if (!processAnnounced.get() && terminalReceived.compareAndSet(false, true)) {
                    val message = "The Android game process did not start within 20 seconds."
                    logger.error("runtime", message, details = mapOf("instanceId" to preparedLaunch.instanceId))
                    trySend(LaunchEvent.Failed(message))
                    close()
                }
            }
        } catch (error: Exception) {
            logger.error("runtime", "Android game activity could not start", error)
            trySend(LaunchEvent.Failed(error.message ?: "The Android game activity could not start."))
            close(error)
        }
        awaitClose {
            applicationContext.sendBroadcast(
                Intent(AndroidGameLaunchProtocol.ACTION_STOP).apply {
                    setPackage(applicationContext.packageName)
                    putExtra(AndroidGameLaunchProtocol.EXTRA_LAUNCH_ID, launchId)
                },
            )
        }
    }

    private fun requireSupportedInstance(instance: GameInstance) {
        unavailableReason?.let { throw LauncherException.RuntimeUnavailable(it) }
        if (instance.minecraftVersionId != MVP_VERSION || instance.modLoader != ModLoader.VANILLA) {
            throw LauncherException.RuntimeUnavailable(
                "The Android MVP supports vanilla Minecraft $MVP_VERSION only.",
            )
        }
    }

    private fun net.blockhost.trestle.download.DownloadProgress.toRuntimeProgress(
        stage: String,
    ): RuntimePreparationProgress = RuntimePreparationProgress(
        stage = activeLabel ?: stage,
        completedBytes = completedBytes,
        totalBytes = totalBytes,
        completedItems = completedFiles,
        totalItems = totalFiles,
    )

    private fun unexpectedProcessDeath(
        preparedLaunch: PreparedLaunch,
        launchId: String,
        processId: Int,
        launchStartedAt: Long,
    ): String {
        val gameDirectory = File(preparedLaunch.workingDirectory)
        val launchDiagnostics = File(gameDirectory, ".trestle/crashes/$launchId")
            .listFiles()
            .orEmpty()
            .asSequence()
        val gameCrashReports = File(gameDirectory, "crash-reports")
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.lastModified() >= launchStartedAt }
        val diagnostics = (launchDiagnostics + gameCrashReports)
            .filter(File::isFile)
            .maxByOrNull(File::lastModified)
        val systemExit = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                applicationContext.getSystemService(ActivityManager::class.java)
                    .getHistoricalProcessExitReasons(applicationContext.packageName, processId, 4)
                    .firstOrNull { it.pid == processId }
            } else {
                null
            }
        }.getOrNull()
        runCatching {
            systemExit?.traceInputStream?.bufferedReader()?.useLines { lines ->
                lines.take(MAX_SYSTEM_TRACE_LINES).forEach { line ->
                    logger.error(
                        "minecraft-native",
                        line,
                        details = mapOf("instanceId" to preparedLaunch.instanceId),
                    )
                }
            }
        }
        return if (diagnostics != null) {
            "Minecraft stopped unexpectedly. Crash details were saved to ${diagnostics.absolutePath}."
        } else if (systemExit != null) {
            val reason = when (systemExit.reason) {
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "a native crash"
                ApplicationExitInfo.REASON_CRASH -> "a JVM crash"
                ApplicationExitInfo.REASON_ANR -> "an application-not-responding event"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "Android low-memory termination"
                else -> "Android exit reason ${systemExit.reason}"
            }
            "Minecraft stopped because of $reason${systemExit.description?.let { ": $it" }.orEmpty()}. " +
                "Review the launcher log for captured exit details."
        } else {
            "Minecraft stopped unexpectedly. Review the streamed launcher log for the last JVM or native message."
        }
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
                "classpath_separator" to ANDROID_CLASSPATH_SEPARATOR,
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

    private fun androidEnvironment(runtimeHome: String, nativeDirectory: String): Map<String, String> = mapOf(
        "AMETHYST_RENDERER" to "opengles3_desktopgl_zink_kopper",
        "FORCE_VSYNC" to "true",
        "GALLIUM_DRIVER" to "zink",
        "HOME" to applicationContext.filesDir.absolutePath,
        "JAVA_HOME" to runtimeHome,
        "LIBGL_DRIVERS_PATH" to nativeDirectory,
        "LIBGL_ES" to "3",
        "LIBGL_MIPMAP" to "3",
        "LIBGL_NOERROR" to "1",
        "LIBGL_NOINTOVLHACK" to "1",
        "LIBGL_NORMALIZE" to "1",
        "LD_LIBRARY_PATH" to "$runtimeHome/lib/server:$runtimeHome/lib:$nativeDirectory",
        "MESA_ANDROID_NO_KMS_SWRAST" to "1",
        "MESA_GL_VERSION_OVERRIDE" to "4.6COMPAT",
        "MESA_GLSL_CACHE_DIR" to applicationContext.cacheDir.absolutePath,
        "MESA_GLSL_VERSION_OVERRIDE" to "460",
        "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
        "PATH" to "$runtimeHome/bin:${System.getenv("PATH").orEmpty()}",
        "POJAVEXEC_EGL" to "libEGL_mesa.so",
        "POJAV_NATIVEDIR" to nativeDirectory,
        "POJAV_VSYNC_IN_ZINK" to "1",
        "TMPDIR" to applicationContext.cacheDir.absolutePath,
        "allow_glsl_extension_directive_midshader" to "true",
        "allow_higher_compat_version" to "true",
        "force_glsl_extensions_warn" to "true",
    )

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
            val availableSession = requireNotNull(session)
            return availableSession.accessToken?.let(CommandArgument::Secret) ?: CommandArgument.Public("0")
        }
        return CommandArgument.Public(substitutePublic(argument, values))
    }

    private fun CommandArgument.reveal(): String = when (this) {
        is CommandArgument.Public -> value
        is CommandArgument.Secret -> value.reveal()
        is CommandArgument.RequiredCredential -> throw LauncherException.AuthenticationRequired()
    }

    private companion object {
        const val MVP_VERSION = "26.2"
        const val REQUIRED_JAVA_MAJOR = 25
        const val ANDROID_CLASSPATH_SEPARATOR = ":"
        const val PROCESS_POLL_MILLIS = 500L
        const val EXIT_DIAGNOSTICS_DELAY_MILLIS = 300L
        const val PROCESS_START_TIMEOUT_MILLIS = 20_000L
        const val MAX_SYSTEM_TRACE_LINES = 80
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

object AndroidGameLaunchProtocol {
    const val GAME_ACTIVITY_CLASS = "net.blockhost.trestle.game.MinecraftGameActivity"
    const val ACTION_STOP = "net.blockhost.trestle.action.STOP_GAME"

    const val EXTRA_LAUNCH_ID = "launch_id"
    const val EXTRA_RECEIVER = "receiver"
    const val EXTRA_RUNTIME_HOME = "runtime_home"
    const val EXTRA_WORKING_DIRECTORY = "working_directory"
    const val EXTRA_NATIVE_DIRECTORY = "native_directory"
    const val EXTRA_JVM_ARGUMENTS = "jvm_arguments"
    const val EXTRA_MAIN_CLASS = "main_class"
    const val EXTRA_GAME_ARGUMENTS = "game_arguments"
    const val EXTRA_ENVIRONMENT = "environment"
    const val EXTRA_PROCESS_ID = "process_id"
    const val EXTRA_EXIT_CODE = "exit_code"
    const val EXTRA_MESSAGE = "message"

    const val RESULT_STARTED = 1
    const val RESULT_LOG = 2
    const val RESULT_EXITED = 3
    const val RESULT_FAILED = 4
    const val RESULT_PROCESS_CREATED = 5
}
