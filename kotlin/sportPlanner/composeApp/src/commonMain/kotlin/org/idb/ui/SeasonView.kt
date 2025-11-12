package org.idb.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ryinex.kotlin.datatable.data.DataTable
import com.ryinex.kotlin.datatable.data.DataTableColumnLayout
import com.ryinex.kotlin.datatable.data.DataTableConfig
import com.ryinex.kotlin.datatable.data.DataTableEditTextConfig
import com.ryinex.kotlin.datatable.data.setList
import com.ryinex.kotlin.datatable.data.text
import com.ryinex.kotlin.datatable.views.DataTableView
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import java.text.ParseException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.idb.database.AppDatabase
import org.idb.database.Competition
import org.idb.database.Season
import org.idb.database.SeasonCompetition
import org.idb.database.SeasonDao
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class SeasonViewModel : BaseViewModel<SeasonDao, Season>() {
    override fun getDao(db: AppDatabase): SeasonDao = db.getSeasonDao()
}

private val editor : Editors = Editors.SEASONS

@Serializable
data class SeasonCompetitionParam(val seasonId : Short, val seasonName : String, val competitionId : Short, val competitionName : String)

@Composable
fun navigateSeason(navController : NavController, argument : String?) {
    when (argument) {
        "View" -> seasonListView(navController)
        "Add" -> seasonEditor(navController)
        else -> seasonEditor(navController, Json.decodeFromString<Season>(argument!!))
    }
}

@Composable
@Preview
private fun seasonListView(navController: NavController) {
    val viewModel: SeasonViewModel = koinInject()
    val competitionViewModel: CompetitionViewModel = koinInject()
    val seasonCompetitionViewModel: SeasonCompetitionViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()
    val competitionState = competitionViewModel.uiState.collectAsState()
    val seasonCompetitionState = seasonCompetitionViewModel.uiState.collectAsState()
    val mergedState = object : BaseUiState {
        override fun loadingInProgress(): Boolean = state.value.isLoading || competitionState.value.isLoading || seasonCompetitionState.value.isLoading

        override fun hasData(): Boolean = state.value.data != null && competitionState.value.data != null && seasonCompetitionState.value.data != null
    }
    viewCommon(mergedState, navController, "Seasons", { createFloatingAction(navController, editor.addRoute()) }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(
                items = createSeasonsList(viewModel.uiState.value.data!!, competitionState.value.data!!, seasonCompetitionState.value.data!!),
                key = { pair -> pair.first }) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        when (val entity = pair.second) {
                            is Season -> {
                                Row(modifier = Modifier.weight(2F), content = {
                                    spacedViewText(entity.name)
                                })

                                itemButtons(
                                    editClick = {
                                        navController.navigate(editor.editRoute(entity))
                                    },
                                    deleteClick = {
                                        coroutineScope.launch {
                                            viewModel.delete(entity)
                                        }
                                    })
                            }

                            is SeasonCompetition -> {
                                Row(modifier = Modifier.weight(2F), content = {
                                    Spacer(Modifier.size(32.dp))
                                    spacedViewText(competitionState.value.data?.first { it.id == entity.competitionId }?.name!!)
                                    spacedViewText(convertMillisToDate(entity.startDate))
                                    spacedViewText("to")
                                    spacedViewText(convertMillisToDate(entity.endDate))
                                })

                                spacedIcon(Icons.Default.Info, "manage season breaks") {
                                    navController.navigate(Editors.SEASON_BREAK.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }

                                spacedIcon(Icons.Default.Settings, "manage teams") {
                                    navController.navigate(Editors.SEASON_TEAM_CATEGORY.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }

                                spacedIcon(Icons.Default.Face, "manage teams") {
                                    navController.navigate(Editors.SEASON_TEAMS.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }
                            }
                        }
                    })

            }
        })
    })
}

private fun createSeasonCompetitionParam(state: State<UiState<Season>>, entity: SeasonCompetition, data: List<Competition>?)
    : SeasonCompetitionParam {
    val seasonName = state.value.data?.find { it.id == entity.seasonId }?.name!!
    val competitionName = data?.find { it.id == entity.competitionId }?.name!!
    return SeasonCompetitionParam(entity.seasonId, seasonName, entity.competitionId, competitionName)
}

// ensure all competitions for all seasons are present (so adding a competition appears in the list without an entry in season competitions)
private fun createSeasonsList(seasons : List<Season>, competitions : List<Competition>, seasonCompetitions : List<SeasonCompetition>) : List<Pair<Int, Any>> {
    val orderedCompetitions = competitions.sortedBy { it.name.trim().uppercase() }
    val orderedSeasons = seasons.sortedByDescending { it.name.trim().uppercase() }
    val seasonCompetitionMap = mutableMapOf<Pair<Short, Short>, SeasonCompetition>()

    for (sc in seasonCompetitions) {
        seasonCompetitionMap[Pair(sc.seasonId, sc.competitionId)] = sc
    }

    val result = mutableListOf<Pair<Int, Any>>()
    for (s in orderedSeasons) {
        result += Pair(result.size, s)
        for (c in orderedCompetitions) {
            val key = Pair(s.id, c.id)
            val sc = seasonCompetitionMap[key]

            result += Pair(result.size, sc ?: SeasonCompetition(seasonId = s.id, competitionId = c.id, startDate = 0, endDate = 0))
        }
    }

    return result
}


private enum class DateOption {START, END}
private data class Dates(var start : Long, var end : Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun seasonEditor(navController: NavController, season : Season? = null) {
    val viewModel: SeasonViewModel = koinInject<SeasonViewModel>()
    val seasonCompetitionModel = koinInject<SeasonCompetitionViewModel>()
    val seasonCompetitionState = seasonCompetitionModel.uiState.collectAsState()
    val competitionModel : CompetitionViewModel = koinInject<CompetitionViewModel>()
    val competitionState = competitionModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(season?.name ?: "") }
    var editConfig by remember {
        val config = DataTableEditTextConfig.default<String, Competition>(
            isEditable = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            maxLines = 1
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

    if (seasonCompetitionState.value.isLoading || competitionState.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        val competitionList = competitionState.value.data?.sortedBy { it.name.trim().uppercase() }
        val dates = competitionList?.associateBy({ it.id }, { Dates(0L, 0L) })
        if (season != null) {
            for (current in seasonCompetitionState.value.data!!) {
                if (current.seasonId == season.id) {
                    dates?.get(current.competitionId)?.start = current.startDate
                    dates?.get(current.competitionId)?.end = current.endDate
                }
            }
        }
        val title = if (season == null) "Add Season" else "Edit Season"
        PreferableMaterialTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { createTopBar(navController, title, "Return to Seasons") },
                content = { paddingValues ->
                    Column (content = {
                        Row(
                            modifier = Modifier.padding(paddingValues), content = {
                                ViewTextField(value = name, label = "Name :") { name = it }
                            })
                        seasonCompetitionDataTable(
                            competitionList!!, config, editConfig, { id -> dates?.get(id)!! },
                            { id, option, text ->
                                run {
                                    var result : String? = null
                                    try {
                                        val milli = dateToMillis(text)
                                        when (option) {
                                            DateOption.START -> dates?.get(id)?.start = milli
                                            DateOption.END -> dates?.get(id)?.end = milli
                                        }
                                        result = convertMillisToDate(milli)
                                    } catch (_ : ParseException) {
                                        // ignore
                                    }
                                    result
                                }
                            })
                    })
                }, bottomBar = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val seasonId = viewModel.insert(Season(id = season?.id ?: 0.toShort(), name = name.trim()))
                                for (date in dates!!) {
                                    val entity = SeasonCompetition(
                                        seasonId = seasonId.toShort(),
                                        competitionId = date.key,
                                        startDate = date.value.start,
                                        endDate = date.value.end
                                    )
                                    seasonCompetitionModel.insert(
                                        entity
                                    )
                                }
                                navController.popBackStack()
                            }
                        },
                        enabled = !name.isEmpty() /*&& startDate > 0 && endDate > startDate*/
                    ) { androidx.compose.material.Text(stringResource(Res.string.ok)) }
                })
        }
    }
}

@Composable
private fun seasonCompetitionDataTable(
    competitionList: List<Competition>,
    config: DataTableConfig,
    editConfig: DataTableEditTextConfig<String, Competition>,
    dateFunction: (Short) -> Dates,
    editFunction: (Short, DateOption, String) -> String?
) {
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val table = remember {
        val table = DataTable<Competition>(config = config, scope = scope, lazyState = lazyState)

        table
            .text(name = "Competition", value = { _, data -> data.name })
            .text(name = "Start date",
                editTextConfig = editConfig.copy(onConfirmEdit = { data, _, text -> editFunction(data.id, DateOption.START, text)}),
                value = { _, data -> convertMillisToDate(dateFunction(data.id).start)})
            .text(name = "End date",
                editTextConfig = editConfig.copy(onConfirmEdit = { data, _, text -> editFunction(data.id, DateOption.END, text)}),
                value = { _, data -> convertMillisToDate(dateFunction(data.id).end)})
            .setList(list = competitionList, key = { _, competition -> competition.id })
    }

    LaunchedEffect(true) {
        table.enableInteractions(true)
    }

    LaunchedEffect(config) {
        table.setConfig(config)
    }

    DataTableView(table = table)
}