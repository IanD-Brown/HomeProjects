package io.github.iandbrown.sportplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.iandbrown.sportplanner.database.builder
import io.github.iandbrown.sportplanner.di.startKoinCommon
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startKoinCommon(builder())

        setContent {
            App(FileKitDialogSettings.createDefault())
        }
    }
}
