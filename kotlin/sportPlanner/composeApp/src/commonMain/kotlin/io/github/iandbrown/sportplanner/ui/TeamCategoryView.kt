package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
private fun TeamCategoryEditor(navController: NavController) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val state = viewModel.uiState.collectAsState()

    ViewCommon(
        state.value,
        navController,
        "Team Categories",
        { CreateFloatingAction(navController, editor.addRoute()) }) { paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            item { ViewText("Name") }
            item { ViewText("Match Day") }
            item {}
            item {}
            for (teamCategory in state.value.data?.sortedBy { it.name.uppercase().trim() }!!) {
                item { ViewText(teamCategory.name) }
                item { ViewText(Day.entries[teamCategory.matchDay.toInt()].display) }
                item { EditButton {navController.navigate(editor.editRoute(teamCategory)) } }
                item { DeleteButton { viewModel.delete(teamCategory) } }
            }
        }
    }
}

@Composable
private fun EditTeamCategory(navController: NavController, editCategory: TeamCategory?) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var matchDay by remember { mutableIntStateOf(editCategory?.matchDay?.toInt() ?: 0) }
    val title = if (editCategory == null) "Add TeamCategory" else "Edit TeamCategory"

    ViewCommon(SimpleState(), navController, title, {}, "Return to teamCategories",
        {
            Button(onClick = {
                save(coroutineScope, viewModel, editCategory, name, matchDay)
                navController.popBackStack()
            },
            enabled = name.isNotEmpty()) { ViewText("OK") }
        },
        {name.isNotEmpty() && (editCategory == null || name != editCategory.name) || (editCategory != null && matchDay.toShort() != editCategory.matchDay)},
        {save(coroutineScope, viewModel, editCategory, name, matchDay)}) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText(value = "Name") }
            item { ReadonlyViewText(value = "Match Day") }
            item { ViewTextField(value = name, onValueChange = { name = it }) }
            item { DropdownList(
                itemList = Day.entries.map { it.display },
                selectedIndex = matchDay,
            ) { matchDay = it } }
        }
    }
}

private fun save(coroutineScope: CoroutineScope, viewModel: TeamCategoryViewModel, editCategory: TeamCategory?, name: String, matchDay: Int) =
    coroutineScope.launch {
        viewModel.insert(
            TeamCategory(
                editCategory?.id ?: 0.toShort(),
                name.trim(),
                matchDay.toShort()
            )
        )
    }
