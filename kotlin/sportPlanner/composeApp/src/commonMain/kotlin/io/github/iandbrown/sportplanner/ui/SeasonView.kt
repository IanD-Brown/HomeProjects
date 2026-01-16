package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled._123
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonDao
import io.github.iandbrown.sportplanner.logic.DayDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class SeasonViewModel : BaseViewModel<SeasonDao, Season>() {
    override fun getDao(db: AppDatabase): SeasonDao = db.getSeasonDao()
}

class SeasonCompViewModel : ReadonlyViewModel<SeasonCompViewDao, SeasonCompView>() {
    override fun getDao(db: AppDatabase): SeasonCompViewDao = db.getSeasonCompViewDao()
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
private fun SeasonListView(navController: NavController) {
    val viewModel: SeasonViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val seasonCompViewState = koinInject<SeasonCompViewModel>().uiState.collectAsState()

    ViewCommon(
        MergedState(state.value, seasonCompViewState.value),
        navController,
        "Seasons",
        {  },
        bottomBar = { BottomBarWithButton("+") {navController.navigate(editor.addRoute())} },
        content = { paddingValues ->
            var seasonId : Short? = null

            LazyVerticalGrid(WeightedIconGridCells(3, 1, 2), modifier = Modifier.padding(paddingValues)) {
                for (seasonCompView in seasonCompViewState.value.data!!) {
                    if (seasonCompView.seasonId != seasonId) {
                        item { ViewText(seasonCompView.seasonName) }
                        item { ViewText("") }

                        item { ClickableIcon(Icons.Default.Splitscreen, "manage season breaks") {
                            navController.navigate(Editors.SEASON_BREAK.viewRoute(Season(seasonCompView.seasonId, seasonCompView.seasonName)))
                        }}
                        item { EditButton { navController.navigate(editor.editRoute(Season(seasonCompView.seasonId, seasonCompView.seasonName))) }}
                        item { DeleteButton { viewModel.delete(Season(seasonCompView.seasonId, seasonCompView.seasonName)) }}

                        seasonId = seasonCompView.seasonId
                    }
                    item { ViewText(" * ${seasonCompView.competitionName}") }
                    item { val startDate = DayDate(seasonCompView.startDate).toString()
                        val endDate = DayDate(seasonCompView.endDate).toString()
                        val join = if (startDate.isNotBlank() || endDate.isNotBlank()) "to" else ""
                        ViewText("$startDate $join $endDate") }
                    item {
                        if (seasonCompView.competitionType == CompetitionTypes.KNOCK_OUT_CUP.ordinal.toShort()) {
                            ClickableIcon(Icons.Default.Rotate90DegreesCcw, "manage season competition rounds") {
                                navController.navigate(Editors.SEASON_COMPETITION_ROUND.viewRoute(seasonCompetitionParamOf(seasonCompView)))
                            }
                        } else {
                            Icon(Blank, "")

                        }
                    }
                    item { ClickableIcon(Icons.Default.Accessibility, "manage teams") {
                        navController.navigate(Editors.SEASON_TEAM_CATEGORY.viewRoute(seasonCompetitionParamOf(seasonCompView)))
                    }}
                    item { ClickableIcon(Icons.Default._123, "manage match structure") {
                        navController.navigate(Editors.SEASON_TEAMS.viewRoute(seasonCompetitionParamOf(seasonCompView)))
                    }}
                }
            }
    })
}

private fun seasonCompetitionParamOf(seasonCompView : SeasonCompView) : SeasonCompetitionParam =
    SeasonCompetitionParam(seasonCompView.seasonId, seasonCompView.seasonName, seasonCompView.competitionId, seasonCompView.competitionName)

@Composable
private fun SeasonEditor(navController: NavController, season : Season? = null) {
    val database = koinInject<AppDatabase>()
    val viewModel: SeasonViewModel = koinInject()
    val seasonParameter = parametersOf(season?.id ?: 0)
    val seasonCompetitionModel: SeasonCompetitionViewModel = koinInject {seasonParameter}
    val seasonCompetitionState = seasonCompetitionModel.uiState.collectAsState()
    val competitionState = koinInject<CompetitionViewModel>().uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(season?.name ?: "") }
    val title = if (season == null) "Add Season" else "Edit Season"
    val startDates = remember { mutableStateMapOf<Short, Int>() }
    val endDates = remember { mutableStateMapOf<Short, Int>() }

    ViewCommon(
        MergedState(competitionState.value, seasonCompetitionState.value),
        navController,
        title,
        {},
        "Return to seasons",
        {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton("ok", Modifier.weight(1f), name.isNotBlank()) {
                    save(coroutineScope, season, viewModel, name, competitionState, startDates, endDates, database)
                    navController.popBackStack()
                }
            }
        },
        {
            if (season == null) {
                name.isNotEmpty()
            } else if (season.name != name) {
                true
            } else {
                seasonCompetitionState.value.data?.filter { it.seasonId == season.id }
                    ?.filter { it.startDate != startDates[it.competitionId] }
                    ?.any { it.endDate != endDates[it.competitionId] }!!
            }
        },
        {save(coroutineScope, season, viewModel, name, competitionState, startDates, endDates, database)},
        content = { paddingValues ->
            val competitionList = competitionState.value.data?.sortedBy { it.name.trim().uppercase() }
            if (season != null) {
                for (current in seasonCompetitionState.value.data!!) {
                    if (current.seasonId == season.id) {
                        startDates[current.competitionId] = current.startDate
                        endDates[current.competitionId] = current.endDate
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText("Name:") }
                item { ViewTextField(value = name, onValueChange = { name = it }) }
                item { ReadonlyViewText("") }
                item { ReadonlyViewText("Competition") }
                item { ReadonlyViewText("Start") }
                item { ReadonlyViewText("End") }
                for (competition in competitionList!!) {
                    item { ReadonlyViewText(competition.name) }
                    item {
                        DatePickerView(
                            current = startDates[competition.id] ?: 0,
                            modifier = Modifier,
                            isSelectable = {
                                val dayDate = DayDate(it)
                                dayDate.isMonday() && dayDate.value() < (endDates[competition.id] ?: Integer.MAX_VALUE) }) {
                            startDates[competition.id] = it
                        }
                    }
                    item {
                        DatePickerView(
                            current = endDates[competition.id] ?: 0,
                            modifier = Modifier,
                            isSelectable = { val dayDate = DayDate(it)
                                dayDate.isSunday() && dayDate.value() > (startDates[competition.id] ?: 0) }) {
                            endDates[competition.id] = it
                        }
                    }
                }
            }
        })
}

private fun save(
    coroutineScope: CoroutineScope,
    season: Season?,
    viewModel: SeasonViewModel,
    name: String,
    competitionState: State<UiState<Competition>>,
    startDates: SnapshotStateMap<Short, Int>,
    endDates: SnapshotStateMap<Short, Int>,
    appDataBase: AppDatabase
) {
    coroutineScope.launch {
        val seasonId = when (season) {
            null -> viewModel.insert(Season(name = name.trim()))
            else -> {
                viewModel.update(Season(season.id, name.trim()))
                season.id
            }
        }
        for (competition in competitionState.value.data!!) {
            val seasonCompetition = SeasonCompetition(
                seasonId = seasonId.toShort(),
                competitionId = competition.id,
                startDate = startDates[competition.id] ?: 0,
                endDate = endDates[competition.id] ?: 0
            )
            if (seasonCompetition.isValid()) {
                appDataBase.getSeasonCompetitionDao().insert(seasonCompetition)
            }
        }
    }
}
