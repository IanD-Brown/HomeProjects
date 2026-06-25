package io.github.iandbrown.home_energy

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.api.LoggerFactory
import dev.shivathapaa.logger.core.LogEvent
import dev.shivathapaa.logger.core.LoggerConfig
import dev.shivathapaa.logger.formatters.LogEventFormatter
import dev.shivathapaa.logger.sink.DefaultLogSink
import dev.shivathapaa.logger.sink.LogSink
import io.github.iandbrown.home_energy.database.builder
import io.github.iandbrown.home_energy.database.getAppDataFolder
import io.github.iandbrown.home_energy.di.startKoinCommon
import io.github.iandbrown.home_energy.utils.LogFormatter
import java.io.File

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    val formatter = LogFormatter()
    LoggerFactory.install(
        LoggerConfig.Builder()
            .minLevel(LogLevel.DEBUG)
            .addSink(DefaultLogSink(formatter))
            .addSink(FileSink("${getAppDataFolder()}/SportLog.txt", formatter))
            .build()
    )
    application {
        startKoinCommon(builder())
        Window(onCloseRequest = ::exitApplication, title = "Home energy") {
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
