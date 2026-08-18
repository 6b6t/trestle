package net.blockhost.trestle.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import net.blockhost.trestle.auth.AuthSession
import net.blockhost.trestle.auth.SessionProvider
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.install.LauncherDirectories
import net.blockhost.trestle.logging.LauncherLogger
import net.blockhost.trestle.logging.NoopLauncherLogger
import net.blockhost.trestle.metadata.InstalledVersion
import net.blockhost.trestle.metadata.MavenCoordinate
import net.blockhost.trestle.metadata.OperatingSystem
import net.blockhost.trestle.metadata.PlatformEnvironment
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class DesktopMinecraftRuntime(
    private val environment: PlatformEnvironment,
    private val directories: LauncherDirectories,
    private val sessionProvider: SessionProvider,
    private val installedVersionReader: (GameInstance) -> InstalledVersion,
    private val javaResolver: JavaResolver,
    private val logger: LauncherLogger = NoopLauncherLogger,
) : MinecraftRuntime {
    override val capabilities = RuntimeCapabilities(
        canPrepareLaunch = true,
        canLaunch = true,
        supportsManagedJava = true,
        supportsNativeExtraction = true,
        supportsCustomJava = true,
    )

    override suspend fun prepare(
        instance: GameInstance,
        options: LaunchOptions,
        onProgress: suspend (RuntimePreparationProgress) -> Unit,
    ): PreparedLaunch =
        withContext(Dispatchers.IO) {
            val session = sessionProvider.currentSession()
            val installed = installedVersionReader(instance)
            val java = instance.javaExecutable ?: javaResolver.resolve(
                component = installed.metadata.javaVersion?.component,
                requiredMajor = installed.requiredJavaMajor,
            )
            if (instance.javaExecutable != null) {
                val executable = Path.of(java)
                if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
                    throw LauncherException.RuntimeUnavailable("The custom Java executable is not usable: $java")
                }
            }
            val nativeDirectory = extractNatives(instance, installed)
            val clientJar = directories.versions / instance.minecraftVersionId / "${instance.minecraftVersionId}.jar"
            val classpathEntries = installed.libraries.filter { !it.native && it.classpath }
                .map { (directories.libraries / it.path).toString() } + clientJar.toString()
            val separator = if (environment.operatingSystem == OperatingSystem.WINDOWS) ";" else ":"
            val classpath = classpathEntries.joinToString(separator)
            val values = launchValues(instance, installed, session, nativeDirectory.toString(), classpath, separator)

            val jvmArguments = buildList {
                add("-Xms${instance.memory.minimumMiB}M")
                add("-Xmx${instance.memory.maximumMiB}M")
                if (installed.jvmArguments.isEmpty()) {
                    add("-Djava.library.path=$nativeDirectory")
                    add("-cp")
                    add(classpath)
                } else {
                    addAll(installed.jvmArguments.map { substitutePublic(it, values) })
                }
                installed.loggingPath?.let { loggingPath ->
                    val configuration = installed.metadata.logging["client"]
                    if (configuration != null) {
                        add(configuration.argument.replace("\${path}", (directories.logging / loggingPath).toString()))
                    }
                }
                addAll(JvmArgumentPolicy.review(instance.jvmArguments).accepted)
                addAll(JvmArgumentPolicy.review(options.additionalJvmArguments).accepted)
                addAll(loaderBootstrapArguments(instance, installed, clientJar))
                if (session?.authenticationMethod == net.blockhost.trestle.auth.AccountAuthenticationMethod.THE_ALTENING) {
                    addAll(THE_ALTENING_ENVIRONMENT_ARGUMENTS)
                }
            }
            val gameArguments = installed.gameArguments + instance.gameArguments + options.additionalGameArguments
            val commandArguments = buildList {
                jvmArguments.forEach { add(CommandArgument.Public(it)) }
                add(CommandArgument.Public(installed.metadata.mainClass))
                gameArguments.forEach { argument -> add(substituteArgument(argument, values, session)) }
                if (options.demo) add(CommandArgument.Public("--demo"))
            }
            PreparedLaunch(
                instanceId = instance.id.value,
                executable = java,
                arguments = commandArguments,
                workingDirectory = (instance.instanceDirectory.toPath() / "game").toString(),
                environment = instance.environmentVariables,
                mainClass = installed.metadata.mainClass,
                classpathEntries = classpathEntries,
                nativeDirectory = nativeDirectory.toString(),
                missingRequirements = if (session == null) listOf("Java account") else emptyList(),
            ).also { prepared ->
                logger.info(
                    "runtime",
                    "Prepared Minecraft launch",
                    mapOf(
                        "instanceId" to instance.id.value,
                        "mainClass" to prepared.mainClass,
                        "classpathEntries" to prepared.classpathEntries.size,
                        "authenticated" to (session != null),
                    ),
                )
            }
        }

    override fun launch(preparedLaunch: PreparedLaunch): Flow<LaunchEvent> = channelFlow {
        var process: Process? = null
        try {
            val command = listOf(preparedLaunch.executable) + preparedLaunch.processArguments()
            process = ProcessBuilder(command)
                .directory(File(preparedLaunch.workingDirectory))
                .redirectErrorStream(true)
                .apply { environment().putAll(preparedLaunch.environment) }
                .start()
            val processId = runCatching { process.pid() }.getOrNull()
            logger.info(
                "runtime",
                "Minecraft process started",
                mapOf("instanceId" to preparedLaunch.instanceId, "processId" to processId),
            )
            send(LaunchEvent.Started(processId))
            val outputJob = launch(Dispatchers.IO) {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        logger.debug("minecraft", line, mapOf("instanceId" to preparedLaunch.instanceId))
                        send(LaunchEvent.Log(line))
                    }
                }
            }
            val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }
            outputJob.join()
            val details = mapOf("instanceId" to preparedLaunch.instanceId, "exitCode" to exitCode)
            if (exitCode == 0) {
                logger.info("runtime", "Minecraft process exited", details)
            } else {
                logger.warn("runtime", "Minecraft process exited with an error", details = details)
            }
            send(LaunchEvent.Exited(exitCode))
        } catch (error: CancellationException) {
            process?.destroy()
            logger.info("runtime", "Minecraft launch cancelled", mapOf("instanceId" to preparedLaunch.instanceId))
            trySend(LaunchEvent.Cancelled)
            throw error
        } catch (error: Exception) {
            process?.destroy()
            logger.error(
                "runtime",
                "Minecraft process could not start",
                error,
                mapOf("instanceId" to preparedLaunch.instanceId),
            )
            send(LaunchEvent.Failed(error.message ?: "Minecraft could not start."))
        }
    }

    private fun extractNatives(instance: GameInstance, installed: InstalledVersion): Path {
        val root = Path.of(instance.instanceDirectory, ".trestle", "natives", installed.metadata.id)
        Files.createDirectories(root)
        for (library in installed.libraries.filter { it.native }) {
            val archive = Path.of((directories.libraries / library.path).toString())
            ZipFile(archive.toFile()).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory || library.extractionExcludes.any { entry.name.startsWith(it) }) continue
                    val target = root.resolve(entry.name).normalize()
                    if (!target.startsWith(root)) {
                        throw LauncherException.FileSystem("A native library contains an unsafe path.")
                    }
                    Files.createDirectories(target.parent)
                    zip.getInputStream(entry).use { input ->
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
        return root
    }

    private fun launchValues(
        instance: GameInstance,
        installed: InstalledVersion,
        session: AuthSession?,
        nativeDirectory: String,
        classpath: String,
        classpathSeparator: String,
    ): Map<String, String> = buildMap {
        putAll(
            mapOf(
                "natives_directory" to nativeDirectory,
                "launcher_name" to "Trestle",
                "launcher_version" to BuildInfo.VERSION,
                "classpath" to classpath,
                "classpath_separator" to classpathSeparator,
                "library_directory" to directories.libraries.toString(),
                "version_name" to installed.metadata.id,
                "game_directory" to (instance.instanceDirectory.toPath() / "game").toString(),
                "assets_root" to directories.assets.toString(),
                "assets_index_name" to (installed.assetIndexId ?: installed.metadata.assets.orEmpty()),
                "version_type" to installed.metadata.type,
                "user_properties" to "{}",
            ),
        )
        if (session != null) {
            put("auth_player_name", session.playerName)
            put("auth_uuid", session.profileId)
            put("user_type", session.userType)
            put("clientid", session.clientId)
            put("auth_xuid", session.xuid)
        }
    }

    private fun substitutePublic(argument: String, values: Map<String, String>): String =
        PLACEHOLDER.replace(argument) { match -> values[match.groupValues[1]] ?: match.value }

    private fun loaderBootstrapArguments(
        instance: GameInstance,
        installed: InstalledVersion,
        clientJar: okio.Path,
    ): List<String> {
        if (instance.modLoader !in setOf(ModLoader.NEOFORGE, ModLoader.FORGE)) return emptyList()
        val loaderVersion = instance.loaderVersion
            ?: throw LauncherException.InvalidMetadata("The ${instance.modLoader.label} instance has no loader version.")
        val installerCoordinate = when (instance.modLoader) {
            ModLoader.FORGE -> "net.minecraftforge:forge:${instance.minecraftVersionId}-$loaderVersion:installer"
            ModLoader.NEOFORGE -> if ("--fml.neoForgeVersion" in installed.gameArguments) {
                "net.neoforged:neoforge:$loaderVersion:installer"
            } else {
                "net.neoforged:forge:${instance.minecraftVersionId}-$loaderVersion:installer"
            }
            else -> error("Loader bootstrap arguments were requested for an unsupported loader.")
        }
        val installer = directories.libraries /
            MavenCoordinate.parse(installerCoordinate).path()
        return listOf(
            "-Dforgewrapper.librariesDir=${directories.libraries}",
            "-Dforgewrapper.minecraft=$clientJar",
            "-Dforgewrapper.installer=$installer",
        )
    }

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

    private companion object {
        val PLACEHOLDER = Regex("\\$\\{([^}]+)}")
        val AUTH_PLACEHOLDERS = listOf(
            "\${auth_player_name}",
            "\${auth_uuid}",
            "\${auth_access_token}",
            "\${user_type}",
            "\${clientid}",
            "\${auth_xuid}",
        )
        val THE_ALTENING_ENVIRONMENT_ARGUMENTS = listOf(
            "-Dminecraft.api.auth.host=http://authserver.thealtening.com",
            "-Dminecraft.api.account.host=http://authserver.thealtening.com",
            "-Dminecraft.api.session.host=http://sessionserver.thealtening.com",
            "-Dminecraft.api.services.host=https://api.minecraftservices.com",
            "-Dminecraft.api.profiles.host=https://api.minecraftservices.com",
        )
    }
}

fun interface JavaResolver {
    suspend fun resolve(component: String?, requiredMajor: Int): String
}
