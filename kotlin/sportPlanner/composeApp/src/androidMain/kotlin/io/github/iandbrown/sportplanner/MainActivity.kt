package io.github.iandbrown.sportplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.iandbrown.sportplanner.database.DBFactory
import io.github.iandbrown.sportplanner.di.startKoinCommon
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startKoinCommon(DBFactory())

        setContent {
            App(FileKitDialogSettings.createDefault())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(FileKitDialogSettings.createDefault())
}
