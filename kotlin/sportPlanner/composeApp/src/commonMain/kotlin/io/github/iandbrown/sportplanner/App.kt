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
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    PreferableMaterialTheme {
        val navController = rememberNavController()

        NavHost(navController, startDestination = "home") {

            composable("home") {
                HomeScreen(navController)
            }

            composable(getRoute(Editors.ASSOCIATIONS)) {
                NavigateAssociation(navController, getArgument(it))
            }

            composable(getRoute(Editors.COMPETITIONS)) {
                NavigateCompetitions(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASONS)) {
                NavigateSeason(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_BREAK)) {
                NavigateSeasonBreak(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_COMPETITION_ROUND)) {
                NavigateSeasonCompetitionRound(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_FIXTURES)) {
                NavigateFixtures(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_TEAMS)) {
                NavigateSeasonTeam(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_TEAM_CATEGORY)) {
                NavigateSeasonTeamCategory(navController, getArgument(it))
            }

            composable(getRoute(Editors.TEAM_CATEGORIES)) {
                NavigateTeamCategory(navController, getArgument(it))
            }
        }
    }
}

private fun getRoute(editor : Editors) : String = editor.name + "/{arg}"

private fun getArgument(entry: NavBackStackEntry): String? = entry.arguments?.read { getString("arg") }