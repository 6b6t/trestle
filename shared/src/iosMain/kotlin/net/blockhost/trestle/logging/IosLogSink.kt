package net.blockhost.trestle.logging

internal class IosLogSink : LogSink {
    override fun write(entry: LogEntry, cause: Throwable?) {
        val details = entry.details.takeIf(Map<String, String>::isNotEmpty)
            ?.entries
            ?.joinToString(prefix = " {", postfix = "}") { (key, value) -> "$key=$value" }
            .orEmpty()
        println("[Trestle/${entry.level}] ${entry.category}: ${entry.message}$details")
        cause?.printStackTrace()
    }
}
