package io.github.iandbrown.sportplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.api.LoggerFactory
import dev.shivathapaa.logger.core.LoggerConfig
import dev.shivathapaa.logger.sink.DefaultLogSink
import io.github.iandbrown.sportplanner.database.builder
import io.github.iandbrown.sportplanner.di.startKoinCommon
import io.github.iandbrown.sportplanner.utils.LogFormatter
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import org.koin.core.context.GlobalContext.getOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (getOrNull() == null) {
            LoggerFactory.install(
                LoggerConfig.Builder()
                    .minLevel(LogLevel.DEBUG)
                    .addSink(DefaultLogSink(LogFormatter()))
                    .build())
            startKoinCommon(builder())
        }

        setContent {
            App(FileKitDialogSettings.createDefault())
        }
    }
}
