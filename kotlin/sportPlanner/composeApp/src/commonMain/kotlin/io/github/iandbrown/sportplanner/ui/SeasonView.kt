package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import com.ryinex.kotlin.datatable.data.DataTableColumnLayout
import com.ryinex.kotlin.datatable.data.DataTableConfig
import com.ryinex.kotlin.datatable.data.composable
import com.ryinex.kotlin.datatable.data.setList
import com.ryinex.kotlin.datatable.data.text
import com.ryinex.kotlin.datatable.views.DataTableView
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonDao
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
fun NavigateSeason(navController : NavController, argument : String?) {
    when (argument) {
        "View" -> SeasonListView(navController)
        "Add" -> SeasonEditor(navController)
        else -> SeasonEditor(navController, Json.decodeFromString<Season>(argument!!))
    }
}

@Composable
@Preview
private fun SeasonListView(navController: NavController) {
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

    ViewCommon(mergedState, navController, "Seasons", { CreateFloatingAction(navController, editor.addRoute()) }, content = { paddingValues ->
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
                                    SpacedViewText(entity.name)
                                })

                                ItemButtons(
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
                                    SpacedViewText(competitionState.value.data?.first { it.id == entity.competitionId }?.name!!)
                                    SpacedViewText(convertMillisToDate(entity.startDate))
                                    SpacedViewText("to")
                                    SpacedViewText(convertMillisToDate(entity.endDate))
                                })

                                SpacedIcon(Icons.Default.Star, "manage season competition rounds") {
                                    navController.navigate(Editors.SEASON_COMPETITION_ROUND.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }

                                SpacedIcon(Icons.Default.Info, "manage season breaks") {
                                    navController.navigate(Editors.SEASON_BREAK.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }

                                SpacedIcon(Icons.Default.Settings, "manage teams") {
                                    navController.navigate(Editors.SEASON_TEAM_CATEGORY.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }

                                SpacedIcon(Icons.Default.Face, "manage teams") {
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

@Composable
private fun SeasonEditor(navController: NavController, season : Season? = null) {
    val viewModel: SeasonViewModel = koinInject()
    val seasonCompetitionModel: SeasonCompetitionViewModel = koinInject()
    val seasonCompetitionState = seasonCompetitionModel.uiState.collectAsState()
    val competitionState = koinInject<CompetitionViewModel>().uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(season?.name ?: "") }
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
    val mergedState = object : BaseUiState {
        override fun loadingInProgress(): Boolean =
            competitionState.value.isLoading || competitionState.value.isLoading || seasonCompetitionState.value.isLoading

        override fun hasData(): Boolean =
            competitionState.value.data != null && competitionState.value.data != null && seasonCompetitionState.value.data != null
    }
    val title = if (season == null) "Add Season" else "Edit Season"
    val dates = remember { mutableStateMapOf<Pair<Short, DateOption>, Long>() }

    PreferableMaterialTheme {
        ViewCommon(
            mergedState,
            navController,
            title,
            { }, "Return to seasons", {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val seasonId = when (season) {
                                null -> viewModel.insert(Season(name = name.trim()))
                                else -> {
                                    viewModel.update(Season(season.id, name.trim()))
                                    season.id
                                }
                            }
                            for (competition in competitionState.value.data!!) {
                                seasonCompetitionModel.insert(SeasonCompetition(
                                    seasonId = seasonId.toShort(),
                                    competitionId = competition.id,
                                    startDate = dates[Pair(competition.id, DateOption.START)] ?: 0,
                                    endDate = dates[Pair(competition.id, DateOption.END)] ?: 0
                                ))
                            }
                            navController.popBackStack()
                        }
                    },
                    enabled = !name.isEmpty()
                ) { ViewText(stringResource(Res.string.ok)) }

            },
            content = { paddingValues ->
                val competitionList = competitionState.value.data?.sortedBy { it.name.trim().uppercase() }
                if (season != null) {
                    for (current in seasonCompetitionState.value.data!!) {
                        if (current.seasonId == season.id) {
                            dates[Pair(current.competitionId, DateOption.START)] = current.startDate
                            dates[Pair(current.competitionId, DateOption.END)] = current.endDate
                        }
                    }
                }
                Column(content = {
                    Row(
                        modifier = Modifier.padding(paddingValues), content = {
                            ViewTextField(value = name, label = "Name :") { name = it }
                        })
                    SeasonCompetitionDataTable(
                        competitionList!!, config,
                        { id, option -> dates[Pair(id, option)] ?: 0},
                        { id, option, milli -> dates[Pair(id, option)] = milli })
                })
            })
    }
}

@Composable
private fun SeasonCompetitionDataTable(
    competitionList: List<Competition>,
    config: DataTableConfig,
    dateFunction: (Short, DateOption) -> Long,
    editFunction: (Short, DateOption, Long) -> Unit
) {
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val table = remember {
        val table = DataTable<Competition>(config = config, scope = scope, lazyState = lazyState)

        table
            .text(name = "Competition", value = { _, data -> data.name })
            .composable(name = "Start date", content = { _, competition ->
                DatePickerView(dateFunction(competition.id, DateOption.START), Modifier, { true})
                { editFunction(competition.id, DateOption.START, it) }
            })
            .composable(name = "End date", content = { _, competition ->
                DatePickerView(dateFunction(competition.id, DateOption.END), Modifier, { true})
                { editFunction(competition.id, DateOption.END, it) }
            })
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