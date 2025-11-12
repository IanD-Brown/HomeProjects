package org.idb.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ryinex.kotlin.datatable.data.*
import com.ryinex.kotlin.datatable.views.DataTableView
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.idb.database.AppDatabase
import org.idb.database.Association
import org.idb.database.SeasonTeam
import org.idb.database.SeasonTeamDao
import org.idb.database.TeamCategory
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class SeasonTeamViewModel : BaseViewModel<SeasonTeamDao, SeasonTeam>() {
    override fun getDao(db: AppDatabase): SeasonTeamDao = db.getSeasonTeamDao()
}

@Composable
fun navigateSeasonTeam(navController : NavController, argument : String?) {
    if (argument != null && argument.startsWith("View&")) {
        seasonTeamEditor(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun seasonTeamEditor(navController: NavController, param : SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject()
    val associationModel : AssociationViewModel = koinInject<AssociationViewModel>()
    val teamCategoryViewModel : TeamCategoryViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()
    val state2 = associationModel.uiState.collectAsState()
    val state3 = teamCategoryViewModel.uiState.collectAsState()
    var isLocked by remember { mutableStateOf(true) }
    val edits by remember { mutableStateOf(mutableMapOf<Pair<Short, Short>, Pair<Short, Short>>()) }
    var buttonText by remember {mutableStateOf("Edit")}
    var editConfig by remember {
        val config = DataTableEditTextConfig.default<String, Short>(
            isEditable = true,
        )
        mutableStateOf(config)
    }
    val tableConfig = DataTableConfig.default(isIndexed = false)
    var config by remember {
        val column = tableConfig.column.copy(layout = DataTableColumnLayout.ScrollableKeepLargest)
        val cell = column.cell.copy(
            enterFocusChild = true,
            textAlign = TextAlign.Right,
            padding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
        )

        mutableStateOf(tableConfig.copy(column = column.copy(cell = cell)))
    }

    viewCommon(state.value, navController, "Season ${param.seasonName} Competition ${param.competitionName} Teams", {
        FloatingActionButton(onClick = {
            if (!isLocked) {
                val seasonId = param.seasonId
                coroutineScope.launch {
                    for (entry in edits) {
                        viewModel.insert(SeasonTeam(seasonId = seasonId,
                            teamCategoryId = entry.key.first,
                            associationId = entry.key.second,
                            competitionId = param.competitionId,
                            count = entry.value.second))
                    }
                }
            }
            if (isLocked) {
                buttonText = ""
                isLocked = false
            } else {
                buttonText = "Edit"
                isLocked = true
            }
        },
            content = {
                ViewText(buttonText)
            }
        )
    }, "Return to Seasons screen") {paddingValues ->
        val teamList = state3.value.data?.sortedBy { it.name.uppercase().trim() }
        val associationList = state2.value.data?.sortedBy {it.name.trim().uppercase()}
        val values = mutableMapOf<Pair<Short, Short>, Short>()
        for (seasonTeam in state.value.data!!) {
            if (seasonTeam.seasonId == param.seasonId) {
                values[Pair(seasonTeam.associationId, seasonTeam.teamCategoryId)] = seasonTeam.count
            }
        }
        Column(modifier = Modifier.padding(paddingValues), content = {
            associationTeamDataTable(values, teamList!!, associationList!!, isLocked, config, editConfig)
            { old, value, teamId, associationId ->
                val key = Pair(teamId, associationId)
                if (!edits.contains(key) || edits[key] == null || edits[key]?.first != value.toShort()) {
                    edits[key] = Pair(old.toShort(), value.toShort())
                    buttonText = "Save"
                    value
                } else {
                    edits.remove(key)
                    buttonText = ""
                    value
                }
            }
        })
    }
}

@Composable
private fun associationTeamDataTable(
    values: MutableMap<Pair<Short, Short>, Short>,
    teamList: List<TeamCategory>,
    associationList: List<Association>,
    isLocked: Boolean,
    config: DataTableConfig,
    editConfig: DataTableEditTextConfig<String, Short>,
    onEdit : (old : String, value : String, teamId : Short, associationId : Short) -> String
) {
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val table = remember {
        val table = DataTable<Short>(config = config, scope = scope, lazyState = lazyState)
        table.text(name = "", value = cellFunction(values, associationList, teamList))
        for (team in teamList) {
            table.text(
                name = team.name,
                editTextConfig =
                    editConfig.copy(onConfirmEdit = { index, old, text ->
                        when (text) {
                        "0" -> onEdit(old, text, team.id, index)
                        "1" -> onEdit(old, text, team.id, index)
                        "2" -> onEdit(old, text, team.id, index)
                        else -> null
                    }}),
                presentation = { _, _, c -> c.copy(textAlign = TextAlign.Right) },
                value = cellFunction(values, associationList, teamList)
            )
        }
        table.setList(list = associationList.map { it.id }, key = rowFunction(associationList))
    }

    LaunchedEffect(isLocked, isLoading) {
        table.enableInteractions(!isLocked && !isLoading)
    }

    LaunchedEffect(config) {
        table.setConfig(config)
    }

    DataTableView(table = table)
}

private fun cellFunction(
    values: MutableMap<Pair<Short, Short>, Short>,
    associationList: List<Association>,
    teamList: List<TeamCategory>
): (DataTableCellLocation, Short) -> String = { location, _ ->
    if (location.columnIndex == 0 && location.layoutRowIndex > 0) {
        associationList[location.layoutRowIndex - 1].name
    } else if (location.layoutRowIndex < associationList.size + 1 && location.columnIndex < teamList.size + 1) {
        val associationId = associationList[location.layoutRowIndex - 1].id
        val teamId = teamList[location.columnIndex - 1].id
        values.getOrDefault(Pair(associationId, teamId), 0).toString()
    } else {
        "??"
    }
}

private fun rowFunction(associationList: List<Association>?): (Int, Short) -> Any =
    { index, _ -> {
        if (associationList == null) {
            ""
        } else {
            associationList[index - 1].name
        }
        }
    }
