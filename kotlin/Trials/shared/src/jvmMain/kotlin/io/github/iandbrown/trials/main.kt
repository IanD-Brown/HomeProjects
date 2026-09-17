package io.github.iandbrown.trials

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.api.LoggerFactory
import dev.shivathapaa.logger.core.LogEvent
import dev.shivathapaa.logger.core.LoggerConfig
import dev.shivathapaa.logger.formatters.LogEventFormatter
import dev.shivathapaa.logger.sink.LogSink
import io.github.iandbrown.trials.di.getAppDataFolder
import io.github.iandbrown.trials.di.initKoin
import io.github.iandbrown.trials.utils.LogFormatter
import java.io.File

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    val formatter = LogFormatter()
    val logDir = File(getAppDataFolder())
    logDir.mkdirs()
    LoggerFactory.install(
        LoggerConfig.Builder()
            .minLevel(LogLevel.DEBUG)
            .addSink(FileSink(File(logDir, "TrialsLog.txt").absolutePath, formatter))
            .build()
    )
    application {
        if (org.koin.core.context.GlobalContext.getOrNull() == null) {
            initKoin()
        }
        Window(onCloseRequest = ::exitApplication, title = "Trials") {
            App()
        }
    }
}

private class FileSink(filename: String, val formatter: LogEventFormatter) : LogSink {
    private val file = File(filename)

    override fun emit(event: LogEvent) {
        file.appendText(formatter.format(event))
    }

    override fun flush() {
        // ensure all data is written
    }
}
