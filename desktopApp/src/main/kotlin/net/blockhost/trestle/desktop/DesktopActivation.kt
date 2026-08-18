package net.blockhost.trestle.desktop

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.io.path.absolute
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal sealed interface DesktopActivation {
    data object Show : DesktopActivation
    data object OpenSettings : DesktopActivation
    data class ImportFiles(val paths: List<Path>) : DesktopActivation
    data class LaunchInstance(val id: String) : DesktopActivation
}

internal object DesktopActivationParser {
    private val supportedExtensions = setOf("jar", "zip", "mrpack")

    fun parse(arguments: List<String>, workingDirectory: Path = Path.of("")): List<DesktopActivation> {
        if (arguments.isEmpty()) return listOf(DesktopActivation.Show)

        val activations = mutableListOf<DesktopActivation>()
        val importPaths = mutableListOf<Path>()
        var index = 0
        while (index < arguments.size) {
            val argument = arguments[index]
            when {
                argument == "--settings" -> activations += DesktopActivation.OpenSettings
                argument == "--launch-instance" -> {
                    arguments.getOrNull(++index)?.takeIf(String::isNotBlank)?.let { id ->
                        activations += DesktopActivation.LaunchInstance(id)
                    }
                }
                argument.startsWith("trestle:", ignoreCase = true) -> parseTrestleUri(argument)?.let(activations::add)
                else -> parseImportPath(argument, workingDirectory)?.let(importPaths::add)
            }
            index += 1
        }
        if (importPaths.isNotEmpty()) activations.add(0, DesktopActivation.ImportFiles(importPaths.distinct()))
        return activations.ifEmpty { listOf(DesktopActivation.Show) }
    }

    private fun parseImportPath(value: String, workingDirectory: Path): Path? {
        val path = runCatching {
            if (value.startsWith("file:", ignoreCase = true)) Path.of(URI(value)) else Path.of(value)
        }.getOrNull() ?: return null
        val absolute = if (path.isAbsolute) path else workingDirectory.resolve(path).absolute()
        return absolute.takeIf { it.isRegularFile() && it.extension.lowercase() in supportedExtensions }
    }

    private fun parseTrestleUri(value: String): DesktopActivation? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (!uri.scheme.equals("trestle", ignoreCase = true)) return null
        val route = buildList {
            uri.host?.takeIf(String::isNotBlank)?.let(::add)
            addAll(uri.path.orEmpty().split('/').filter(String::isNotBlank))
        }
        return when (route.firstOrNull()?.lowercase()) {
            "settings" -> DesktopActivation.OpenSettings
            "launch" -> route.getOrNull(1)?.let(DesktopActivation::LaunchInstance)
            else -> DesktopActivation.Show
        }
    }
}

/**
 * Keeps one desktop launcher process alive and forwards later file/deep-link activations to it.
 */
internal class DesktopActivationBroker private constructor(
    private val directory: Path,
    private val lockChannel: FileChannel,
    private val lock: FileLock,
    private val server: ServerSocket,
    private val token: String,
) : AutoCloseable {
    private val pending = ConcurrentLinkedQueue<DesktopActivation>()
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "trestle-activation-listener").apply { isDaemon = true }
    }
    @Volatile
    private var handler: ((DesktopActivation) -> Unit)? = null

    init {
        executor.execute(::listen)
    }

    fun enqueue(arguments: List<String>) {
        DesktopActivationParser.parse(arguments).forEach(::dispatch)
    }

    fun setHandler(handler: (DesktopActivation) -> Unit) {
        this.handler = handler
        while (true) handler(pending.poll() ?: break)
    }

    private fun dispatch(activation: DesktopActivation) {
        val currentHandler = handler
        if (currentHandler == null) pending += activation else currentHandler(activation)
    }

    private fun listen() {
        while (!server.isClosed) {
            val socket = runCatching { server.accept() }.getOrNull() ?: break
            runCatching { receive(socket) }
        }
    }

    private fun receive(socket: Socket) = socket.use {
        val input = DataInputStream(it.getInputStream())
        val output = DataOutputStream(it.getOutputStream())
        if (input.readUTF() != token) return@use
        val count = input.readInt().coerceIn(0, MAX_ARGUMENTS)
        val arguments = List(count) { input.readUTF() }
        enqueue(arguments)
        output.writeBoolean(true)
        output.flush()
    }

    override fun close() {
        handler = null
        runCatching { server.close() }
        executor.shutdownNow()
        runCatching {
            val endpoint = readEndpoint(directory)
            if (endpoint?.token == token) Files.deleteIfExists(endpointPath(directory))
        }
        runCatching { lock.release() }
        runCatching { lockChannel.close() }
    }

    companion object {
        private const val MAX_ARGUMENTS = 64
        private const val FORWARD_TIMEOUT_MILLIS = 2_000L

        /** Returns a broker for the primary process, or null after forwarding to it. */
        fun acquire(arguments: List<String>): DesktopActivationBroker? {
            val directory = activationDirectory()
            Files.createDirectories(directory)
            val channel = FileChannel.open(
                directory.resolve("instance.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            val lock = runCatching { channel.tryLock() }.getOrNull()
            if (lock == null) {
                channel.close()
                forwardWithRetry(directory, arguments)
                return null
            }

            val server = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
            val token = UUID.randomUUID().toString()
            writeEndpoint(directory, Endpoint(server.localPort, token))
            return DesktopActivationBroker(directory, channel, lock, server, token).also {
                it.enqueue(arguments)
            }
        }

        private fun activationDirectory(): Path {
            val user = System.getProperty("user.name", "user").hashCode().toUInt().toString(16)
            return Path.of(System.getProperty("java.io.tmpdir"), "trestle-$user")
        }

        private fun forwardWithRetry(directory: Path, arguments: List<String>) {
            val deadline = System.currentTimeMillis() + FORWARD_TIMEOUT_MILLIS
            var lastError: Throwable? = null
            do {
                val endpoint = readEndpoint(directory)
                if (endpoint != null) {
                    runCatching { forward(endpoint, arguments) }
                        .onSuccess { return }
                        .onFailure { lastError = it }
                }
                Thread.sleep(50)
            } while (System.currentTimeMillis() < deadline)
            error("The existing Trestle process could not be activated: ${lastError?.message ?: "no endpoint"}")
        }

        private fun forward(endpoint: Endpoint, arguments: List<String>) {
            Socket(InetAddress.getLoopbackAddress(), endpoint.port).use { socket ->
                socket.soTimeout = 1_000
                val output = DataOutputStream(socket.getOutputStream())
                output.writeUTF(endpoint.token)
                output.writeInt(arguments.size.coerceAtMost(MAX_ARGUMENTS))
                arguments.take(MAX_ARGUMENTS).forEach(output::writeUTF)
                output.flush()
                check(DataInputStream(socket.getInputStream()).readBoolean())
            }
        }

        private fun endpointPath(directory: Path) = directory.resolve("endpoint.properties")

        private fun writeEndpoint(directory: Path, endpoint: Endpoint) {
            val temporary = Files.createTempFile(directory, "endpoint-", ".properties")
            Files.newOutputStream(temporary).use { output ->
                Properties().apply {
                    setProperty("port", endpoint.port.toString())
                    setProperty("token", endpoint.token)
                }.store(output, null)
            }
            Files.move(
                temporary,
                endpointPath(directory),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        private fun readEndpoint(directory: Path): Endpoint? = runCatching {
            val properties = Properties().apply {
                Files.newInputStream(endpointPath(directory)).use(::load)
            }
            Endpoint(
                port = properties.getProperty("port").toInt(),
                token = properties.getProperty("token"),
            )
        }.getOrNull()
    }
}

private data class Endpoint(val port: Int, val token: String)
