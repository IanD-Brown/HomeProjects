package io.github.iandbrown.reconciler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.iandbrown.reconciler.database.builder
import io.github.iandbrown.reconciler.di.startKoinCommon
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        startKoinCommon(builder())
        Window(onCloseRequest = ::exitApplication, title = "Spreadsheet reconciler") {
            val fileKitDialogSettings = FileKitDialogSettings(parentWindow = this.window)
            App(fileKitDialogSettings)
        }
    }
}
