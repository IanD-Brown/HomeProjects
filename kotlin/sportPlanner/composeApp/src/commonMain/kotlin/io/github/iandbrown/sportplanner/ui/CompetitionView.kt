package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel

class CompetitionViewModel(dao: CompetitionDao) :
    BaseCRUDViewModel<CompetitionDao, Competition>(dao, { it.get() }) {
    fun save(coroutineScope: CoroutineScope, editCompetition: Competition?, name: String, type: Short) {
        coroutineScope.launch {
            if (editCompetition == null) {
                dao.insert(Competition(name = name.trim(), type = type))
            } else {
                dao.update(Competition(editCompetition.id, name.trim(), type))
            }
        }
    }
}


private const val NAME = "Name"
private const val TYPE = "Type"

enum class CompetitionTypes(val display : String) {
    LEAGUE("League"),
    KNOCK_OUT_CUP("Knockout cup")
}

@Composable
fun CompetitionListScreen() {
    val viewModel: CompetitionViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    CompetitionListContent(
        state = state,
        onExport = {
            exportButtonSettings(coroutineScope, "competitions") {
                toDataFrame(state.values()).writeJson(it)
            }
        },
        onImport = {
            importJsonButtonSettings(viewModel) {
                toCompetition(it)
            }
        },
        onAdd = { appNavigator.navigate(Route.CompetitionEdit(null)) },
        onEdit = { appNavigator.navigate(Route.CompetitionEdit(it)) },
        onDelete = { viewModel.delete(it) }
    )
}

@Composable
private fun CompetitionListContent(
    state: ViewModelState<Competition>,
    onExport: () -> ButtonSettings,
    onImport: () -> ButtonSettings,
    onAdd: () -> Unit,
    onEdit: (Competition) -> Unit,
    onDelete: (Competition) -> Unit
) {
    ViewCommon(
        "Competitions",
        bottomBar = {
            BottomBarWithButtons(
                onExport(),
                onImport(),
                addButtonSettings { onAdd() }
            )
        },
        states = persistentListOf(state)
    ) { paddingValues ->
        val competitions = state.values().sortedBy { it.name.uppercase().trim() }.toImmutableList()
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Name", "Type"))
            item(span = { GridItemSpan(2) }) {}
            for (competition in competitions) {
                viewTextItems(listOf(competition.name, CompetitionTypes.entries[competition.type.toInt()].display))
                editButton { onEdit(competition) }
                deleteButton { onDelete(competition) }
            }
        }
    }
}

internal fun toDataFrame(competitions: List<Competition>): DataFrame<Competition> =
    competitions.toDataFrame {
        NAME from { it.name }
        TYPE from { it.type }
    }

internal fun toCompetition(row: DataRow<Any?>): Competition =
    Competition(name = row[NAME] as String, type = (row[TYPE] as Int).toShort())

@Composable
fun CompetitionEditScreen(editCompetition: Competition?) {
    val viewModel: CompetitionViewModel = koinViewModel()
    val coroutineScope = rememberCoroutineScope()

    CompetitionEditContent(
        editCompetition = editCompetition,
        onSave = { name, type ->
            viewModel.save(coroutineScope, editCompetition, name, type)
            appNavigator.goBack()
        },
        onConfirmSave = { name, type -> viewModel.save(coroutineScope, editCompetition, name, type) }
    )
}

@Composable
private fun CompetitionEditContent(
    editCompetition: Competition?,
    onSave: (String, Short) -> Unit,
    onConfirmSave: (String, Short) -> Unit
) {
    var name by remember { mutableStateOf(editCompetition?.name ?: "") }
    var type by remember { mutableStateOf(editCompetition?.type ?: 0.toShort()) }
    val title = if (editCompetition == null) "Add Competition" else "Edit Competition"

    ViewCommon(
        title,
        description = "Return to Competitions",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                onSave(name, type)
            }
        },
        confirm = {
            (name.isNotEmpty() && (editCompetition == null || name != editCompetition.name)) || (editCompetition != null && type != editCompetition.type)
        },
        confirmAction = { onConfirmSave(name, type) },
        states = persistentListOf()
    ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText(value = "Name") }
            item { ReadonlyViewText(value = "Type") }
            item { ViewTextField(value = name, onValueChange = { name = it }) }
            item {
                DropdownList(
                    itemList = CompetitionTypes.entries.map { it.display }.toImmutableList(),
                    selectedIndex = type.toInt(),
                ) { type = it.toShort() }
            }
        }
    }
}
