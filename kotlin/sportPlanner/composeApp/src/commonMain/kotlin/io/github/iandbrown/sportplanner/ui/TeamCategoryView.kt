package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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

class TeamCategoryViewModel(dao: TeamCategoryDao) :
    BaseCRUDViewModel<TeamCategoryDao, TeamCategory>(dao, { it.get() }) {

    fun save(editCategory: TeamCategory?, name: String, matchDay: Int) {
        insert(TeamCategory(editCategory?.id ?: 0.toShort(), name.trim(), matchDay.toShort()))
    }
}

private const val NAME = "Name"
private const val MATCH_DAY = "MatchDay"

private enum class Day(val display: String) {
    MON("Mon"),
    TUES("Tues"),
    WEDS("Weds"),
    THURS("Thurs"),
    FRI("Fri"),
    SAT("Sat"),
    SUN("Sun")
}

@Composable
fun TeamCategoryListScreen() {
    val viewModel: TeamCategoryViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    TeamCategoryListContent(
        state = state,
        onExport = {
            exportButtonSettings(coroutineScope, "teamCategories") {
                toDataFrame(state.values()).writeJson(it)
            }
        },
        onImport = {
            importJsonButtonSettings(viewModel) {
                toTeamCategory(it)
            }
        },
        onAdd = { appNavigator.navigate(Route.TeamCategoryEdit(null)) },
        onEdit = { appNavigator.navigate(Route.TeamCategoryEdit(it)) },
        onDelete = { viewModel.delete(it) }
    )
}

@Composable
private fun TeamCategoryListContent(
    state: ViewModelState<TeamCategory>,
    onExport: () -> ButtonSettings,
    onImport: () -> ButtonSettings,
    onAdd: () -> Unit,
    onEdit: (TeamCategory) -> Unit,
    onDelete: (TeamCategory) -> Unit
) {
    ViewCommon(
        "Team Categories",
        bottomBar = {
            BottomBarWithButtons(
                onExport(),
                onImport(),
                addButtonSettings { onAdd() }
            )
        },
        states = persistentListOf(state)
    ) { paddingValues ->
        val categories = state.values().sortedBy { it.name.uppercase().trim() }.toImmutableList()
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Name", "Match Day"))
            item(span = { GridItemSpan(2) }) {}
            for (teamCategory in categories) {
                viewTextItems(listOf(teamCategory.name, Day.entries[teamCategory.matchDay.toInt()].display))
                editButton { onEdit(teamCategory) }
                deleteButton { onDelete(teamCategory) }
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
fun TeamCategoryEditScreen(editCategory: TeamCategory?) {
    val viewModel: TeamCategoryViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()

    TeamCategoryEditContent(
        editCategory = editCategory,
        state = state,
        onSave = { name, matchDay ->
            viewModel.save(editCategory, name, matchDay)
            appNavigator.goBack()
        },
        onConfirmSave = { name, matchDay ->
            viewModel.save(editCategory, name, matchDay)
        }
    )
}

@Composable
private fun TeamCategoryEditContent(
    editCategory: TeamCategory?,
    state: ViewModelState<TeamCategory>,
    onSave: (String, Int) -> Unit,
    onConfirmSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var matchDay by remember { mutableIntStateOf(editCategory?.matchDay?.toInt() ?: 0) }

    ViewCommon(
        if (editCategory == null) "Add TeamCategory" else "Edit TeamCategory",
        description = "Return to teamCategories",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                onSave(name, matchDay)
            }
        },
        confirm = {
            name.isNotEmpty() && (editCategory == null || name != editCategory.name) ||
                    (editCategory != null && matchDay.toShort() != editCategory.matchDay)
        },
        confirmAction = { onConfirmSave(name, matchDay) },
        states = persistentListOf(state)
    ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText(value = "Name") }
            item { ReadonlyViewText(value = "Match Day") }
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
