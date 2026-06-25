package io.github.iandbrown.home_energy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.shivathapaa.logger.api.LogLevel
import dev.shivathapaa.logger.api.LoggerFactory
import dev.shivathapaa.logger.core.LoggerConfig
import dev.shivathapaa.logger.sink.DefaultLogSink
import io.github.iandbrown.home_energy.database.builder
import io.github.iandbrown.home_energy.di.startKoinCommon
import io.github.iandbrown.home_energy.utils.LogFormatter
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
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
