package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class TeamCategoryViewModel : BaseViewModel<TeamCategoryDao, TeamCategory>() {
    override fun getDao(db: AppDatabase): TeamCategoryDao = db.getTeamCategoryDao()
}

private val editor = Editors.TEAM_CATEGORIES
private enum class Day(val display : String) {
    MON("Mon"),
    TUES("Tues"),
    WEDS("Weds"),
    THURS("Thurs"),
    FRI("Fri"),
    SAT("Sat"),
    SUN("Sun")
}

@Composable
fun NavigateTeamCategory(navController : NavController, argument : String?) {
    when (argument) {
        "View" -> TeamCategoryEditor(navController)
        "Add" -> EditTeamCategory(navController, null)
        else -> EditTeamCategory(navController, Json.decodeFromString<TeamCategory>(argument!!))
    }
}

@Composable
@Preview
fun TeamCategoryEditor(navController: NavController) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()

    ViewCommon(state.value, navController, "Team Categories", { CreateFloatingAction(navController, editor.addRoute()) }) {
            paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            val values = state.value.data!!
            items(
                items = values.sortedBy { it.name.uppercase().trim() },
                key = { teamCategory -> teamCategory.id }) { teamCategory ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        Row(
                            modifier = Modifier.weight(2F), content = {
                                Spacer(modifier = Modifier.size(16.dp))
                                ViewText(teamCategory.name)
                                Spacer(modifier = Modifier.size(16.dp))
                                ViewText(Day.entries[teamCategory.matchDay.toInt()].display)
                            })

                        ItemButtons(
                            editClick = {
                                navController.navigate(
                                    Editors.TEAM_CATEGORIES.name +
                                            "/${Json.encodeToString(teamCategory)}"
                                )
                            },
                            deleteClick = {
                                coroutineScope.launch {
                                    viewModel.delete(teamCategory)
                                }
                            })
                    })

            }
        })
    }
}

@Composable
fun EditTeamCategory(navController: NavController, editCategory: TeamCategory?) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var matchDay by remember { mutableIntStateOf(editCategory?.matchDay?.toInt() ?: 0) }
    val title = if (editCategory == null) "Add TeamCategory" else "Edit TeamCategory"

    ViewCommon(SimpleState(), navController, title, {}, "Return to teamCategories", {
        Button(onClick = {
            coroutineScope.launch {
                viewModel.insert(TeamCategory(editCategory?.id ?: 0.toShort(), name.trim(), matchDay.toShort()))
                navController.popBackStack()
            }
        },
            enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }
    }) { paddingValues ->
        PreferableMaterialTheme {
            FlowRow(modifier = Modifier.padding(paddingValues).fillMaxWidth(), maxItemsInEachRow = 2) {
                ViewText("Name", modifier = Modifier.weight(1f))
                ViewText("Match Day", modifier = Modifier.weight(1f))
                ViewTextField(value = name, onValueChange = { name = it }, modifier = Modifier.weight(1f))
                DropdownList(
                    itemList = Day.entries.map { it.display },
                    selectedIndex = matchDay,
                    modifier = Modifier.weight(1f)
                ) { matchDay = it }
            }
        }
    }
}
