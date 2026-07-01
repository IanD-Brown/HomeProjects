package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.inject

class CompetitionViewModel(dao: CompetitionDao) :
    BaseCRUDViewModel<CompetitionDao, Competition>(dao, {it.get()})


private val editor = Editors.COMPETITIONS
private const val NAME = "Name"
private const val TYPE = "Type"

@Composable
fun NavigateCompetitions(argument: String?) {
    when (argument) {
        "View" -> CompetitionView()
        "Add" -> EditCompetition(null)
        else -> EditCompetition(Json.decodeFromString<Competition>(argument!!))
    }
}

enum class CompetitionTypes(val display : String) {
    LEAGUE("League"),
    KNOCK_OUT_CUP("Knockout cup")
}

@Suppress("ParamsComparedByRef")
@Composable
private fun CompetitionView(viewModel: CompetitionViewModel = koinViewModel()) {
    val state = viewModel.getState().collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Competitions",
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "competitions") {
                    toDataFrame(state.values()).writeJson(it)
                },
                importJsonButtonSettings(viewModel) {
                    toCompetition(it)
                },
                addButtonSettings { it.navigate(editor.addRoute()) }
            )
        }, states = persistentListOf(state.value)) { paddingValues ->
            LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
                viewTextItems(listOf("Name", "Type"))
                item(span = { GridItemSpan(2) }) {}
                for (competition in state.values().sortedBy { it.name.uppercase().trim() }) {
                    viewTextItems(listOf(competition.name, CompetitionTypes.entries[competition.type.toInt()].display) )
                    editButton {editor.editRoute(competition) }
                    deleteButton { viewModel.delete(competition) }
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
private fun EditCompetition(editCompetition: Competition?) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editCompetition?.name ?: "") }
    var type by remember {mutableStateOf(editCompetition?.type ?: 0.toShort())}
    val title = if (editCompetition == null) "Add Competition" else "Edit Competition"

    ViewCommon(
        title, description = "Return to Competitions", bottomBar = {
            BottomBarWithButton(enabled = !name.isEmpty()) {
                save(coroutineScope, editCompetition, name, type)
                appNavController.popBackStack()
            }
        }, confirm = {
            (name.isNotEmpty() && (editCompetition == null || name != editCompetition.name)) || (editCompetition != null && type != editCompetition.type)
        },
        confirmAction = {save(coroutineScope, editCompetition, name, type)},
        states = persistentListOf()) { paddingValues ->
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText(value = "Name") }
                item { ReadonlyViewText(value = "Type") }
                item { ViewTextField(value = name, onValueChange = { name = it }) }
                item { DropdownList(
                    itemList = CompetitionTypes.entries.map { it.display }.toImmutableList(),
                    selectedIndex = type.toInt(),
                ) { type = it.toShort() } }
            }
        }
}

private fun save(coroutineScope: CoroutineScope,
                 editCompetition: Competition?,
                 name: String, type: Short,
                 dao: CompetitionDao = inject<CompetitionDao>(CompetitionDao::class.java).value) {
    coroutineScope.launch {
        if (editCompetition == null) {
            dao.insert(Competition(name = name.trim(), type = type))
        } else {
            dao.update(Competition(editCompetition.id, name.trim(), type))
        }
    }
}
