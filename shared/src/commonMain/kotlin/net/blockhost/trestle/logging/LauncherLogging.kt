package net.blockhost.trestle.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

data class LogEntry(
    val id: Long,
    val timestampEpochMillis: Long,
    val level: LogLevel,
    val category: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

fun interface LogSink {
    fun write(entry: LogEntry, cause: Throwable?)
}

interface LauncherLogger {
    val entries: StateFlow<List<LogEntry>>

    fun debug(category: String, message: String, details: Map<String, Any?> = emptyMap())
    fun info(category: String, message: String, details: Map<String, Any?> = emptyMap())
    fun warn(category: String, message: String, cause: Throwable? = null, details: Map<String, Any?> = emptyMap())
    fun error(category: String, message: String, cause: Throwable? = null, details: Map<String, Any?> = emptyMap())
    fun clear()
    fun configure(capacity: Int, stopOnOverflow: Boolean) {}
}

class BufferedLauncherLogger(
    private val nowMillis: () -> Long,
    private val sink: LogSink,
    capacity: Int = 300,
) : LauncherLogger {
    @Volatile
    private var capacity: Int = capacity

    @Volatile
    private var stopOnOverflow: Boolean = false
    init {
        require(capacity > 0)
    }

    private val mutableEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    private var nextId = 1L
    override val entries: StateFlow<List<LogEntry>> = mutableEntries.asStateFlow()

    override fun debug(category: String, message: String, details: Map<String, Any?>) =
        append(LogLevel.DEBUG, category, message, null, details)

    override fun info(category: String, message: String, details: Map<String, Any?>) =
        append(LogLevel.INFO, category, message, null, details)

    override fun warn(category: String, message: String, cause: Throwable?, details: Map<String, Any?>) =
        append(LogLevel.WARN, category, message, cause, details)

    override fun error(category: String, message: String, cause: Throwable?, details: Map<String, Any?>) =
        append(LogLevel.ERROR, category, message, cause, details)

    override fun clear() {
        mutableEntries.value = emptyList()
    }

    override fun configure(capacity: Int, stopOnOverflow: Boolean) {
        require(capacity > 0)
        this.capacity = capacity
        this.stopOnOverflow = stopOnOverflow
        if (mutableEntries.value.size > capacity) {
            mutableEntries.value = mutableEntries.value.takeLast(capacity)
        }
    }

    private fun append(
        level: LogLevel,
        category: String,
        message: String,
        cause: Throwable?,
        details: Map<String, Any?>,
    ) {
        val entry = LogEntry(
            id = nextId++,
            timestampEpochMillis = nowMillis(),
            level = level,
            category = category,
            message = LogRedactor.redact(message),
            details = details.mapValues { (key, value) ->
                if (LogRedactor.isSensitiveKey(key)) "[REDACTED]" else LogRedactor.redact(value?.toString().orEmpty())
            },
        )
        if (stopOnOverflow && mutableEntries.value.size >= capacity) return
        mutableEntries.value = (mutableEntries.value + entry).takeLast(capacity)
        sink.write(entry, cause)
    }
}

object NoopLauncherLogger : LauncherLogger {
    private val emptyEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    override val entries: StateFlow<List<LogEntry>> = emptyEntries.asStateFlow()

    override fun debug(category: String, message: String, details: Map<String, Any?>) = Unit
    override fun info(category: String, message: String, details: Map<String, Any?>) = Unit
    override fun warn(category: String, message: String, cause: Throwable?, details: Map<String, Any?>) = Unit
    override fun error(category: String, message: String, cause: Throwable?, details: Map<String, Any?>) = Unit
    override fun clear() = Unit
}

internal object LogRedactor {
    private val namedSecret = Regex(
        "(?i)(authorization|access[ _-]?token|refresh[ _-]?token|client[ _-]?secret)([=:\\s\"']+)([^\\s,}\"]+)",
    )
    private val bearerToken = Regex("(?i)bearer\\s+[^\\s,}]+")
    private val jwt = Regex("eyJ[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]{12,}(?:\\.[A-Za-z0-9_-]{8,})?")

    fun redact(value: String): String = value
        .replace(bearerToken, "Bearer [REDACTED]")
        .replace(namedSecret) { "${it.groupValues[1]}${it.groupValues[2]}[REDACTED]" }
        .replace(jwt, "[REDACTED]")

    fun isSensitiveKey(key: String): Boolean = key.lowercase().let {
        it.contains("token") || it.contains("authorization") || it.contains("secret") || it.contains("password")
    }
}
