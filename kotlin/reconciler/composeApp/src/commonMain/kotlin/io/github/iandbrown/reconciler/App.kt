package io.github.iandbrown.reconciler

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.read
import com.softartdev.theme.material3.PreferableMaterialTheme
import io.github.iandbrown.reconciler.database.ImportDefinition
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.ui.EditImportDefinition
import io.github.iandbrown.reconciler.ui.EditRule
import io.github.iandbrown.reconciler.ui.Editors
import io.github.iandbrown.reconciler.ui.HomeScreen
import io.github.iandbrown.reconciler.ui.ImportDefinitionListView
import io.github.iandbrown.reconciler.ui.ViewAllTransaction
import io.github.iandbrown.reconciler.ui.NavigateRule
import io.github.iandbrown.reconciler.ui.ViewSpendingSummary
import io.github.iandbrown.reconciler.ui.ViewTransactionSummaryByCategory
import io.github.iandbrown.reconciler.ui.appFileKitDialogSettings
import io.github.iandbrown.reconciler.ui.appNavController
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

@Suppress("ParamsComparedByRef")
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

            composable(getRoute(Editors.ALL_TRANSACTIONS)) {
                ViewAllTransaction()
            }

            composable(getRoute(Editors.SUMMARY_BY_CATEGORY)) {
                ViewTransactionSummaryByCategory()
            }

            composable(getRoute(Editors.SPENDING_SUMMARY)) {
                ViewSpendingSummary()
            }

            composable(getRoute(Editors.IMPORT_DEFINITION)) {
                ImportDefinitionListView()
            }

            composable<Rule> {
                EditRule(it.toRoute())
            }

            composable<ImportDefinition> {
                EditImportDefinition(it.toRoute())
            }
        }
    }
}

private fun getRoute(editor : Editors) : String = "${editor.name}/{arg}"

private fun getArgument(entry: NavBackStackEntry): String? = entry.arguments?.read { getString("arg") }
