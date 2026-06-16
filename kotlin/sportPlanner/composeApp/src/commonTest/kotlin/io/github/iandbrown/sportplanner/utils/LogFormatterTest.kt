package io.github.iandbrown.sportplanner.utils

import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.core.LogContext
import dev.shivathapaa.logger.core.LogEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import java.time.Instant

class LogFormatterTest : BehaviorSpec({
    val formatter = LogFormatter()

    given("a basic LogEvent") {
        val timestamp = Instant.parse("2025-01-01T10:00:00Z").toEpochMilli()
        val event = LogEvent(
            level = LogLevel.INFO,
            message = "Hello World",
            loggerName = "TestLogger",
            thread = "main",
            timestamp = timestamp,
            attributes = emptyMap(),
            context = LogContext(emptyMap()),
            throwable = null
        )

        When("formatting the event") {
            val result = formatter.format(event)

            then("it should contain the level, thread, and logger name") {
                result shouldContain "[INFO]"
                result shouldContain "[main]"
                result shouldContain "TestLogger"
            }

            then("it should contain the message") {
                result shouldContain " - Hello World"
            }
        }
    }

    given("a LogEvent with attributes and context") {
        val event = LogEvent(
            level = LogLevel.DEBUG,
            message = "Test with attributes",
            loggerName = "AttrLogger",
            thread = "worker",
            timestamp = Instant.now().toEpochMilli(),
            attributes = mapOf("key1" to "val1"),
            context = LogContext(mapOf("ctx1" to "ctxVal1")),
            throwable = null
        )

        When("formatting the event") {
            val result = formatter.format(event)

            then("it should contain the attributes") {
                result shouldContain "Attributes:"
                result shouldContain "key1 = val1"
            }

            then("it should contain the context") {
                result shouldContain "Context:"
                result shouldContain "ctx1 = ctxVal1"
            }
        }
    }

    given("a LogEvent with a throwable") {
        val exception = RuntimeException("Something went wrong")
        val event = LogEvent(
            level = LogLevel.ERROR,
            message = "Error occurred",
            loggerName = "ErrorLogger",
            thread = "main",
            timestamp = Instant.now().toEpochMilli(),
            attributes = emptyMap(),
            context = LogContext(emptyMap()),
            throwable = exception
        )

        When("formatting the event") {
            val result = formatter.format(event)

            then("it should contain the stack trace") {
                result shouldContain "java.lang.RuntimeException: Something went wrong"
            }
        }
    }
})
