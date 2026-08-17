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
        }
        when (entry.level) {
            LogLevel.DEBUG -> if (cause == null) logger.debug(message) else logger.debug(message, cause)
            LogLevel.INFO -> if (cause == null) logger.info(message) else logger.info(message, cause)
            LogLevel.WARN -> if (cause == null) logger.warn(message) else logger.warn(message, cause)
            LogLevel.ERROR -> if (cause == null) logger.error(message) else logger.error(message, cause)
        }
    }
}
