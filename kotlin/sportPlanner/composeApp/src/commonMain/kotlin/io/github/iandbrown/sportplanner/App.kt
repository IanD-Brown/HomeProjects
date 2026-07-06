package io.github.iandbrown.sportplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.softartdev.theme.material3.PreferableMaterialTheme
import io.github.iandbrown.sportplanner.ui.Navigator
import io.github.iandbrown.sportplanner.ui.Route
import io.github.iandbrown.sportplanner.ui.appFileKitDialogSettings
import io.github.iandbrown.sportplanner.ui.appNavigator
import io.github.iandbrown.sportplanner.ui.rememberNavigationState
import io.github.iandbrown.sportplanner.ui.toEntries
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import org.koin.compose.navigation3.koinEntryProvider

@Composable
fun App(fileKitDialogSettings: FileKitDialogSettings) {
    appFileKitDialogSettings = fileKitDialogSettings
    PreferableMaterialTheme {
        val navigationState = rememberNavigationState(
            startRoute = Route.Home,
            topLevelRoutes = setOf(Route.Home)
        )

        val navigator = remember { Navigator(navigationState) }
        appNavigator = navigator

        val entryProvider = koinEntryProvider<NavKey>()

        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}
