import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.idb.database.DBFactory
import org.idb.di.startKoinCommon

fun main() = application {
    startKoinCommon(DBFactory())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Football season planner",
    ) {

        App()
    }
}