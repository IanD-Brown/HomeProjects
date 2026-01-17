package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
fun NavigateTeamCategory(argument: String?) {
    when (argument) {
        "View" -> TeamCategoryEditor()
        "Add" -> EditTeamCategory(null)
        else -> EditTeamCategory(Json.decodeFromString<TeamCategory>(argument!!))
    }
}

@Composable
private fun TeamCategoryEditor() {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val state = viewModel.uiState.collectAsState()

    ViewCommon(
        state.value,
        "Team Categories",
        { CreateFloatingAction(editor.addRoute()) },
        content = { paddingValues ->
            LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
                item { ViewText("Name") }
                item { ViewText("Match Day") }
                item {}
                item {}
                for (teamCategory in state.value.data?.sortedBy { it.name.uppercase().trim() }!!) {
                    item { ViewText(teamCategory.name) }
                    item { ViewText(Day.entries[teamCategory.matchDay.toInt()].display) }
                    item { EditButton {editor.editRoute(teamCategory) } }
                    item { DeleteButton { viewModel.delete(teamCategory) } }
                }
            }
        })
}

@Composable
private fun EditTeamCategory(editCategory: TeamCategory?) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var matchDay by remember { mutableIntStateOf(editCategory?.matchDay?.toInt() ?: 0) }
    val title = if (editCategory == null) "Add TeamCategory" else "Edit TeamCategory"

    ViewCommon(
        SimpleState(),
        title,
        description = "Return to teamCategories",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                save(coroutineScope, viewModel, editCategory, name, matchDay)
                appNavController.popBackStack()
            }
        },
        confirm = {name.isNotEmpty() && (editCategory == null || name != editCategory.name) || (editCategory != null && matchDay.toShort() != editCategory.matchDay)},
        confirmAction = {save(coroutineScope, viewModel, editCategory, name, matchDay)},
        content = { paddingValues ->
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText(value = "Name") }
                item { ReadonlyViewText(value = "Match Day") }
                item { ViewTextField(value = name, onValueChange = { name = it }) }
                item { DropdownList(
                    itemList = Day.entries.map { it.display },
                    selectedIndex = matchDay,
                ) { matchDay = it } }
            }
        })
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
