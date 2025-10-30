import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.softartdev.theme.material3.PreferableMaterialTheme
import org.idb.ui.Editors
import org.idb.ui.homeScreen
import org.idb.ui.navigateAssociation
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

            composable(Editors.TEAMCATERORIES.name + "/{arg}",
                listOf(navArgument("arg") { type = NavType.StringType })) {
                val argument = it.arguments?.read { getString("arg") }
                navigateTeamCategory(navController, argument)
            }

            composable(Editors.ASSOCIATIONS.name + "/{arg}") {
                navigateAssociation(navController, it.arguments?.read { getString("arg") })
            }
        }
    }
}