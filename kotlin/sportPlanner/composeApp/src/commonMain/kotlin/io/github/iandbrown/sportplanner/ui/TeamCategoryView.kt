package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel

class  TeamCategoryViewModel(dao : TeamCategoryDao = inject<TeamCategoryDao>().value) :
    BaseConfigCRUDViewModel<TeamCategoryDao, TeamCategory>(dao)

private val editor = Editors.TEAM_CATEGORIES
private const val NAME = "Name"
private const val MATCH_DAY = "MatchDay"

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

@Suppress("ParamsComparedByRef")
@Composable
private fun TeamCategoryEditor(viewModel: TeamCategoryViewModel = koinViewModel()) {
    val state = viewModel.getState().collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Team Categories",
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "teamCategories") {
                    toDataFrame(state.values()).writeJson(it)
                },
                importJsonButtonSettings(viewModel) {
                    toTeamCategory(it)
                },
                addButtonSettings { it.navigate(editor.addRoute()) }
            )
        }, states = persistentListOf(state.value)) { paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Name", "Match Day"))
            item(span = { GridItemSpan(2) }) {}
            for (teamCategory in state.values().sortedBy { it.name.uppercase().trim() }) {
                viewTextItems(listOf(teamCategory.name, Day.entries[teamCategory.matchDay.toInt()].display))
                editButton {editor.editRoute(teamCategory) }
                deleteButton { viewModel.delete(teamCategory) }
            }
        }
    }
}

internal fun toDataFrame(teamCategories: List<TeamCategory>): DataFrame<TeamCategory> =
    teamCategories.toDataFrame {
        NAME from { it.name }
        MATCH_DAY from {it.matchDay}
    }

internal fun toTeamCategory(row: DataRow<Any?>): TeamCategory =
    TeamCategory(name = row[NAME] as String, matchDay = (row[MATCH_DAY] as Int).toShort())

@Composable
private fun EditTeamCategory(editCategory: TeamCategory?) {
    val viewModel: TeamCategoryViewModel = koinViewModel()
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var matchDay by remember { mutableIntStateOf(editCategory?.matchDay?.toInt() ?: 0) }
    val title = if (editCategory == null) "Add TeamCategory" else "Edit TeamCategory"

    ViewCommon(
        title,
        description = "Return to teamCategories",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                save(viewModel, editCategory, name, matchDay)
                appNavController.popBackStack()
            }
        },
        confirm = {name.isNotEmpty() && (editCategory == null || name != editCategory.name) || (editCategory != null && matchDay.toShort() != editCategory.matchDay)},
        confirmAction = {save(viewModel, editCategory, name, matchDay)},
        states = persistentListOf(viewModel.getState().collectAsState().value)) { paddingValues ->
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText(value = "Name") }
                item { ReadonlyViewText(value = "Match Day") }
                item { ViewTextField(value = name, onValueChange = { name = it }) }
                item { DropdownList(
                    itemList = Day.entries.map { it.display }.toImmutableList(),
                    selectedIndex = matchDay,
                ) { matchDay = it } }
            }
        }
}

private fun save(viewModel: TeamCategoryViewModel, editCategory: TeamCategory?, name: String, matchDay: Int) =
    viewModel.insert(TeamCategory(editCategory?.id ?: 0.toShort(), name.trim(), matchDay.toShort()))
