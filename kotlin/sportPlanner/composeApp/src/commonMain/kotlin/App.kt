import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.softartdev.theme.material3.PreferableMaterialTheme
import org.idb.ui.Editors
import org.idb.ui.addAssociation
import org.idb.ui.associationEditor
import org.idb.ui.editAssociation
import org.idb.ui.homeScreen
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

            composable(Editors.ASSOCIATIONS.name) {
                associationEditor(/*viewModel,*/ navController) {
                    navController.navigateUp()
                }
            }

            composable("addAssociation") {
                addAssociation(/*viewModel,*/ navController)
            }

            composable("editAssociation/{name}") {
                editAssociation(navController, it.arguments?.read { getString("name") }!!)
            }
        }
    }
}