package io.github.iandbrown.reconciler

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.read
import com.softartdev.theme.material3.PreferableMaterialTheme
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.ui.EditRule
import io.github.iandbrown.reconciler.ui.Editors
import io.github.iandbrown.reconciler.ui.HomeScreen
import io.github.iandbrown.reconciler.ui.NavigateRule
import io.github.iandbrown.reconciler.ui.NavigateTransaction
import io.github.iandbrown.reconciler.ui.NavigateTransactionByAmount
import io.github.iandbrown.reconciler.ui.appFileKitDialogSettings
import io.github.iandbrown.reconciler.ui.appNavController
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

@Composable
fun App(fileKitDialogSettings: FileKitDialogSettings) {
    appFileKitDialogSettings = fileKitDialogSettings
    PreferableMaterialTheme {
        val navController = rememberNavController()

        appNavController = navController

        NavHost(navController, startDestination = "home") {

            composable("home") {
                HomeScreen()
            }

            composable(getRoute(Editors.RULES)) {
                NavigateRule(getArgument(it))
            }

            composable(getRoute(Editors.TRANSACTIONS)) {
                NavigateTransaction(getArgument(it))
            }

            composable(getRoute(Editors.TRANSACTIONS_BY_AMOUNT)) {
                NavigateTransactionByAmount(getArgument(it))
            }

            composable<Rule> {
                EditRule(it.toRoute())
            }
        }
    }
}

private fun getRoute(editor : Editors) : String = "${editor.name}/{arg}"

private fun getArgument(entry: NavBackStackEntry): String? = entry.arguments?.read { getString("arg") }
