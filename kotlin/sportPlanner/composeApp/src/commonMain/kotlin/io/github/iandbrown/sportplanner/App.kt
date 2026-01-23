package io.github.iandbrown.sportplanner

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.softartdev.theme.material3.PreferableMaterialTheme
import io.github.iandbrown.sportplanner.ui.Editors
import io.github.iandbrown.sportplanner.ui.NavigateAssociation
import io.github.iandbrown.sportplanner.ui.HomeScreen
import io.github.iandbrown.sportplanner.ui.NavigateCompetitions
import io.github.iandbrown.sportplanner.ui.NavigateFixtures
import io.github.iandbrown.sportplanner.ui.NavigateSeason
import io.github.iandbrown.sportplanner.ui.NavigateSeasonBreak
import io.github.iandbrown.sportplanner.ui.NavigateSeasonCompetitionRound
import io.github.iandbrown.sportplanner.ui.NavigateSeasonTeam
import io.github.iandbrown.sportplanner.ui.NavigateSeasonTeamCategory
import io.github.iandbrown.sportplanner.ui.NavigateTeamCategory
import io.github.iandbrown.sportplanner.ui.appFileKitDialogSettings
import io.github.iandbrown.sportplanner.ui.appNavController
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

            composable(getRoute(Editors.ASSOCIATIONS)) {
                NavigateAssociation(getArgument(it))
            }

            composable(getRoute(Editors.COMPETITIONS)) {
                NavigateCompetitions(getArgument(it))
            }

            composable(getRoute(Editors.SEASONS)) {
                NavigateSeason(getArgument(it))
            }

            composable(getRoute(Editors.SEASON_BREAK)) {
                NavigateSeasonBreak(getArgument(it))
            }

            composable(getRoute(Editors.SEASON_COMPETITION_ROUND)) {
                NavigateSeasonCompetitionRound(getArgument(it))
            }

            composable(getRoute(Editors.SEASON_FIXTURES)) {
                NavigateFixtures(getArgument(it))
            }

            composable(getRoute(Editors.SEASON_TEAMS)) {
                NavigateSeasonTeam(getArgument(it))
            }

            composable(getRoute(Editors.SEASON_TEAM_CATEGORY)) {
                NavigateSeasonTeamCategory(getArgument(it))
            }

            composable(getRoute(Editors.TEAM_CATEGORIES)) {
                NavigateTeamCategory(getArgument(it))
            }
        }
    }
}

private fun getRoute(editor : Editors) : String = editor.name + "/{arg}"

private fun getArgument(entry: NavBackStackEntry): String? = entry.arguments?.read { getString("arg") }
