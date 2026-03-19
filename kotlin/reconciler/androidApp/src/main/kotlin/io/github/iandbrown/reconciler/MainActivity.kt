package io.github.iandbrown.reconciler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.iandbrown.reconciler.database.builder
import io.github.iandbrown.reconciler.di.startKoinCommon
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startKoinCommon(builder())
        FileKit.init(this)

        setContent {
            App(FileKitDialogSettings.createDefault())
        }
    }
}
