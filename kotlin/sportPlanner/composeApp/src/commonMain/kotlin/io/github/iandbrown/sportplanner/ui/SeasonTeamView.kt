package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ryinex.kotlin.datatable.data.DataTable
import com.ryinex.kotlin.datatable.data.DataTableCellLocation
import com.ryinex.kotlin.datatable.data.DataTableColumnLayout
import com.ryinex.kotlin.datatable.data.DataTableConfig
import com.ryinex.kotlin.datatable.data.DataTableEditTextConfig
import com.ryinex.kotlin.datatable.data.setList
import com.ryinex.kotlin.datatable.data.text
import com.ryinex.kotlin.datatable.views.DataTableView
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

class SeasonTeamViewModel : BaseViewModel<SeasonTeamDao, SeasonTeam>() {
    override fun getDao(db: AppDatabase): SeasonTeamDao = db.getSeasonTeamDao()
}

@Composable
fun NavigateSeasonTeam(navController: NavController, argument: String?) {
    if (argument != null && argument.startsWith("View&")) {
        SeasonTeamEditor(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonTeamEditor(navController: NavController, param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject()
    val associationModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val teamCategoryViewModel: TeamCategoryViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()
    val state2 = associationModel.uiState.collectAsState()
    val state3 = teamCategoryViewModel.uiState.collectAsState()
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Pair<Short, Short>, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""

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

    ViewCommon(
        state.value,
        navController,
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        {
            FloatingActionButton(
                onClick = {
                    if (!isLocked && edits.isNotEmpty()) {
                        coroutineScope.launch {
                            for ((key, count) in edits) {
                                viewModel.insert(
                                    SeasonTeam(
                                        seasonId = param.seasonId,
                                        teamCategoryId = key.second,
                                        associationId = key.first,
                                        competitionId = param.competitionId,
                                        count = count
                                    )
                                )
                            }
                            edits.clear()
                        }
                    }
                    isLocked = !isLocked
                },
                content = {
                    ViewText(buttonText)
                }
            )
        },
        "Return to Seasons screen"
    ) { paddingValues ->
        val teamList = state3.value.data?.sortedBy { it.name.uppercase().trim() }
        val associationList = state2.value.data?.sortedBy { it.name.trim().uppercase() }
        val values = mutableMapOf<Pair<Short, Short>, Short>()
        for (seasonTeam in state.value.data!!) {
            if (seasonTeam.seasonId == param.seasonId) {
                values[Pair(seasonTeam.associationId, seasonTeam.teamCategoryId)] = seasonTeam.count
            }
        }
        Column(modifier = Modifier.padding(paddingValues), content = {
            if (teamList != null && associationList != null) {
                AssociationTeamDataTable(
                    values,
                    teamList,
                    associationList,
                    isLocked,
                    config,
                    editConfig
                )
                { _, value, teamId, associationId ->
                    val key = Pair(associationId, teamId)
                    val originalValue = values.getOrDefault(key, 0)
                    val newValue = value.toShortOrNull()

                    if (newValue != null && newValue != originalValue) {
                        edits[key] = newValue
                    } else {
                        edits.remove(key)
                    }
                    value
                }
            }
        })
    }
}

@Composable
private fun AssociationTeamDataTable(
    values: Map<Pair<Short, Short>, Short>,
    teamList: List<TeamCategory>,
    associationList: List<Association>,
    isLocked: Boolean,
    config: DataTableConfig,
    editConfig: DataTableEditTextConfig<String, Short>,
    onEdit: (old: String, value: String, teamId: Short, associationId: Short) -> String
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
                        if (text == "0" || text == "1" || text == "2") {
                            onEdit(old, text, team.id, index)
                        } else {
                            null
                        }
                    }),
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
    values: Map<Pair<Short, Short>, Short>,
    associationList: List<Association>,
    teamList: List<TeamCategory>
): (DataTableCellLocation, Short) -> String = { location, _ ->
    if (location.columnIndex == 0 && location.layoutRowIndex > 0) {
        associationList.getOrNull(location.layoutRowIndex - 1)?.name ?: ""
    } else if (location.layoutRowIndex > 0 && location.columnIndex > 0) {
        val associationId = associationList.getOrNull(location.layoutRowIndex - 1)?.id
        val teamId = teamList.getOrNull(location.columnIndex - 1)?.id
        if (associationId != null && teamId != null) {
            values.getOrDefault(Pair(associationId, teamId), 0).toString()
        } else {
            ""
        }
    } else {
        ""
    }
}

private fun rowFunction(associationList: List<Association>?): (Int, Short) -> Any =
    { index, _ ->
        {
            if (associationList == null) {
                ""
            } else {
                associationList.getOrNull(index - 1)?.name ?: ""
            }
        }
    }
