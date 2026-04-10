package io.github.iandbrown.reconciler.utils

import dev.shivathapaa.logger.core.LogEvent
import dev.shivathapaa.logger.formatters.LogEventFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LogFormatter() : LogEventFormatter {
    val timeFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    override fun format(event: LogEvent): String = buildString {
        event.timestamp.let {
            append(timeFormatter.format(Instant.ofEpochMilli(it)))
            append(" ")
        }

        append("[")
        append(event.level.name)
        append("] ")

        append("[")
        append(event.thread)
        append("] ")

        append(event.loggerName)

        event.message?.takeIf { it.isNotBlank() }?.let {
            append(" - ")
            append(it)
        }

        appendAttributes("Attributes", event.attributes)
        appendAttributes("Context", event.context.values)

        event.throwable?.let {
            append('\n')
            append(it.stackTraceToString())
        }
    }

    private fun StringBuilder.appendAttributes(label: String, map: Map<String, Any?>) {
        if (map.isEmpty()) return

        append('\n')
        append("  ")
        append(label)
        append(":")

        map.forEach { (k, v) ->
            append('\n')
            append("    ")
            append(k)
            append(" = ")
            append(v)
        }
    }
}
