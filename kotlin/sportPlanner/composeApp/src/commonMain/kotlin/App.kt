import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.softartdev.theme.material3.PreferableMaterialTheme
import org.idb.ui.Editors
import org.idb.ui.homeScreen
import org.idb.ui.navigateAssociation
import org.idb.ui.navigateCompetitions
import org.idb.ui.navigateSeason
import org.idb.ui.navigateSeasonBreak
import org.idb.ui.navigateSeasonTeam
import org.idb.ui.navigateSeasonTeamCategory
import org.idb.ui.navigateTeamCategory
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    PreferableMaterialTheme {
        val navController = rememberNavController()

        NavHost(navController, startDestination = "home") {

            composable("home") {
                homeScreen(navController)
            }

            composable(getRoute(Editors.ASSOCIATIONS)) {
                navigateAssociation(navController, getArgument(it))
            }

            composable(getRoute(Editors.COMPETITIONS)) {
                navigateCompetitions(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASONS)) {
                navigateSeason(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_BREAK)) {
                navigateSeasonBreak(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_TEAMS)) {
                navigateSeasonTeam(navController, getArgument(it))
            }

            composable(getRoute(Editors.SEASON_TEAM_CATEGORY)) {
                navigateSeasonTeamCategory(navController, getArgument(it))
            }

            composable(getRoute(Editors.TEAM_CATEGORIES)) {
                navigateTeamCategory(navController, getArgument(it))
            }
        }
    }
}

private fun getRoute(editor : Editors) : String = editor.name + "/{arg}"

private fun getArgument(entry: NavBackStackEntry): String? = entry.arguments?.read { getString("arg") }