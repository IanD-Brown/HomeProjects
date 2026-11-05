package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel

internal class TeamCategoryViewModel(dao: TeamCategoryDao) :
    BaseCRUDViewModel<TeamCategoryDao, TeamCategory>(dao, { it.get() })

private const val NAME = "Name"
private const val MATCH_DAY = "MatchDay"

internal enum class Day(val display: String) {
    MON("Mon"),
    TUES("Tues"),
    WEDS("Weds"),
    THURS("Thurs"),
    FRI("Fri"),
    SAT("Sat"),
    SUN("Sun")
}

@Composable
internal fun TeamCategoryListScreen() {
    val viewModel: TeamCategoryViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
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
                addButtonSettings { appNavigator.navigate(Route.TeamCategoryEdit(null)) }
            )
        },
        states = persistentListOf(state)
    ) { paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Name", "Match Day", "", ""))
            state.values().sortedBy { it.name.uppercase().trim() }.forEach {
                viewTextItems(listOf(it.name, Day.entries[it.matchDay.toInt()].display))
                editButton {  appNavigator.navigate(Route.TeamCategoryEdit(it)) }
                deleteButton { viewModel.delete(it) }
            }
        }
    }

}

internal fun toDataFrame(teamCategories: List<TeamCategory>): DataFrame<TeamCategory> =
    teamCategories.toDataFrame {
        NAME from { it.name }
        MATCH_DAY from { it.matchDay }
    }

internal fun toTeamCategory(row: DataRow<Any?>): TeamCategory =
    TeamCategory(name = row[NAME] as String, matchDay = (row[MATCH_DAY] as Int).toShort())

@Composable
internal fun TeamCategoryEditScreen(editCategory: TeamCategory?) {
    val viewModel: TeamCategoryViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var matchDay by remember { mutableIntStateOf(editCategory?.matchDay?.toInt() ?: 0) }

    fun save() {
        if (editCategory == null) {
            viewModel.insert(TeamCategory(name = name, matchDay = matchDay.toShort()))
        } else {
            viewModel.update(editCategory.copy(name = name, matchDay = matchDay.toShort()))
        }
    }

    ViewCommon(
        if (editCategory == null) "Add TeamCategory" else "Edit TeamCategory",
        description = "Return to teamCategories",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                save()
                appNavigator.goBack()
            }
        },
        confirm = {
            name.isNotEmpty() && (editCategory == null || name != editCategory.name) ||
                    (editCategory != null && matchDay.toShort() != editCategory.matchDay)
        },
        confirmAction = { save() },
        states = persistentListOf(state)
    ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Name", "Match Day"))
            item { ViewTextField(value = name, onValueChange = { name = it }) }
            item {
                DropdownList(
                    itemList = Day.entries.map { it.display }.toImmutableList(),
                    selectedIndex = matchDay,
                ) { matchDay = it }
            }
        }
    }
}
