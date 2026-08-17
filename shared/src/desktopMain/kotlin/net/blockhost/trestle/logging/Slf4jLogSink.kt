package net.blockhost.trestle.logging

import org.slf4j.LoggerFactory

class Slf4jLogSink : LogSink {
    override fun write(entry: LogEntry, cause: Throwable?) {
        val logger = LoggerFactory.getLogger("net.blockhost.trestle.${entry.category}")
        val message = buildString {
            append(entry.message)
            if (entry.details.isNotEmpty()) {
                append(" ")
                append(entry.details.entries.joinToString { (key, value) -> "$key=$value" })
            }
            cause?.let {
                append("\n")
                append(it::class.qualifiedName ?: "Throwable")
                it.stackTrace.forEach { frame -> append("\n\tat ").append(frame) }
            }
        }
        when (entry.level) {
            LogLevel.DEBUG -> logger.debug(message)
            LogLevel.INFO -> logger.info(message)
            LogLevel.WARN -> logger.warn(message)
            LogLevel.ERROR -> logger.error(message)
        }
    }
}
