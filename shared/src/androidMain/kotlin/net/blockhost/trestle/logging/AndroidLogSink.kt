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
            cause?.let {
                append("\n")
                append(it::class.qualifiedName ?: "Throwable")
                it.stackTrace.forEach { frame -> append("\n\tat ").append(frame) }
            }
        }
        val tag = "Trestle.${entry.category}".take(23)
        when (entry.level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }
}
