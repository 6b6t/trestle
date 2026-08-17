package net.blockhost.trestle.logging

import android.util.Log

class AndroidLogSink : LogSink {
    override fun write(entry: LogEntry, cause: Throwable?) {
        val message = buildString {
            append(entry.message)
            if (entry.details.isNotEmpty()) {
                append(" ")
                append(entry.details.entries.joinToString { (key, value) -> "$key=$value" })
            }
        }
        val tag = "Trestle.${entry.category}".take(23)
        when (entry.level) {
            LogLevel.DEBUG -> Log.d(tag, message, cause)
            LogLevel.INFO -> Log.i(tag, message, cause)
            LogLevel.WARN -> Log.w(tag, message, cause)
            LogLevel.ERROR -> Log.e(tag, message, cause)
        }
    }
}
