package io.github.iandbrown.reconciler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.api.LoggerFactory
import dev.shivathapaa.logger.core.LogEvent
import dev.shivathapaa.logger.core.LoggerConfig
import dev.shivathapaa.logger.formatters.LogEventFormatter
import dev.shivathapaa.logger.sink.DefaultLogSink
import dev.shivathapaa.logger.sink.LogSink
import io.github.iandbrown.reconciler.database.builder
import io.github.iandbrown.reconciler.database.getAppDataFolder
import io.github.iandbrown.reconciler.di.startKoinCommon
import io.github.iandbrown.reconciler.utils.LogFormatter
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import java.io.File

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    val formatter = LogFormatter()
    LoggerFactory.install(
        LoggerConfig.Builder()
            .minLevel(LogLevel.DEBUG)
            .addSink(DefaultLogSink(formatter))
            .addSink(FileSink("${getAppDataFolder()}/AccountsLog.txt", formatter))
            .build()
    )

    application {
        startKoinCommon(builder())
        Window(onCloseRequest = ::exitApplication, title = "Account reconciler") {
            val fileKitDialogSettings = FileKitDialogSettings(parentWindow = this.window)
            App(fileKitDialogSettings)
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
