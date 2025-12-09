package io.github.iandbrown.sportplanner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.iandbrown.sportplanner.database.DBFactory
import io.github.iandbrown.sportplanner.di.startKoinCommon

fun main() = application {
    startKoinCommon(DBFactory())
    Window(onCloseRequest = ::exitApplication, title = "Football season planner") {
        App()
    }
}
