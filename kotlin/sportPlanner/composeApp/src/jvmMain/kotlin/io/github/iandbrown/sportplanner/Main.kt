package io.github.iandbrown.sportplanner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.iandbrown.sportplanner.database.builder
import io.github.iandbrown.sportplanner.di.startKoinCommon
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        startKoinCommon(builder())
        Window(onCloseRequest = ::exitApplication, title = "Football season planner") {
            val fileKitDialogSettings = FileKitDialogSettings(this.window)
            App(fileKitDialogSettings)
        }
    }
}
