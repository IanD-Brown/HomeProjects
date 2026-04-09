package io.github.iandbrown.reconciler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.api.LoggerFactory
import dev.shivathapaa.logger.core.LoggerConfig
import dev.shivathapaa.logger.formatters.LogFormatters
import dev.shivathapaa.logger.sink.DefaultLogSink
import io.github.iandbrown.reconciler.database.builder
import io.github.iandbrown.reconciler.di.startKoinCommon
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.init
import org.koin.core.context.GlobalContext.getOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (getOrNull() == null) {
            LoggerFactory.install(
                LoggerConfig.Builder()
                    .minLevel(LogLevel.DEBUG)
                    .addSink(DefaultLogSink(LogFormatters.default(showEmoji = false)))
                    .build())
            startKoinCommon(builder())
        }
        FileKit.init(this)

        setContent {
            App(FileKitDialogSettings.createDefault())
        }
    }
}
