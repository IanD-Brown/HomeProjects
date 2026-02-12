package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonDao
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.di.inject
import io.github.iandbrown.sportplanner.logic.DayDate
import kotlin.collections.emptyList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class SeasonViewModel(dao: SeasonDao = inject<SeasonDao>().value) : BaseConfigCRUDViewModel<SeasonDao, Season>(dao) {
    fun saveCompetitions(name: String,
                         competitionState: State<List<Competition>>,
                         startDates: SnapshotStateMap<CompetitionId, Int>,
                         endDates: SnapshotStateMap<CompetitionId, Int>,
                         seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>().value) {
        viewModelScope.launch {
            val seasonId = dao.getSeasonId(name.trim())!!
            for (competition in competitionState.value) {
                val seasonCompetition = SeasonCompetition(
                    seasonId = seasonId.toShort(),
                    competitionId = competition.id,
                    startDate = startDates[competition.id] ?: 0,
                    endDate = endDates[competition.id] ?: 0
                )
                if (seasonCompetition.isValid()) {
                    seasonCompetitionDao.insert(seasonCompetition)
                }
            }
        }
    }
}

class SeasonCompViewModel(dao: SeasonCompViewDao = inject<SeasonCompViewDao>().value) :
    BaseReadViewModel<SeasonCompViewDao, SeasonCompView>(dao) {
    fun deleteSeason(seasonId : SeasonId) {
        viewModelScope.launch {
            dao.deleteSeason(seasonId)
        }
    }

    fun getBySeason(seasonId : SeasonId) : Flow<List<SeasonCompView>> =
        flow {emit(dao.getAsList(seasonId))}
            .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
}

private val editor : Editors = Editors.SEASONS

@Serializable
data class SeasonCompetitionParam(val seasonId : SeasonId, val seasonName : String, val competitionId : CompetitionId, val competitionName : String)

@Composable
fun NavigateSeason(argument: String?) {
    when (argument) {
        "View" -> SeasonListView()
        "Add" -> SeasonEditor()
        else -> SeasonEditor(Json.decodeFromString<Season>(argument!!))
    }
}

@Composable
private fun SeasonListView() {
    val seasonCompViewModel = koinInject<SeasonCompViewModel>()
    val seasonCompViewState = seasonCompViewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    ViewCommon(
        "Seasons",
        bottomBar = { BottomBarWithButtonN("+") {editor.addRoute()} },
        content = { paddingValues ->
            var seasonId : Short? = null

            LazyVerticalGrid(WeightedIconGridCells(3, 1, 2), modifier = Modifier.padding(paddingValues), state = gridState) {
                for (seasonCompView in seasonCompViewState.value) {
                    if (seasonCompView.seasonId != seasonId) {
                        item(key = "${seasonCompView.seasonId}") { ViewText(seasonCompView.seasonName)  }
                        item(key = "${seasonCompView.seasonId}Blank") { ViewText("") }

                        item(key = "${seasonCompView.seasonId}Breaks") { ClickableIcon(Icons.Default.Splitscreen, "manage season breaks") {
                            Editors.SEASON_BREAK.viewRoute(Season(seasonCompView.seasonId, seasonCompView.seasonName))
                        }}
                        item(key = "${seasonCompView.seasonId}Edit") { EditButton { editor.editRoute(Season(seasonCompView.seasonId, seasonCompView.seasonName)) }}
                        item { DeleteButton { seasonCompViewModel.deleteSeason(seasonCompView.seasonId) }}

                        seasonId = seasonCompView.seasonId
                    }
                    item(key = "${seasonCompView.seasonId}-${seasonCompView.competitionId}") { ViewText(" * ${seasonCompView.competitionName}") }
                    item(key = "${seasonCompView.seasonId}-${seasonCompView.competitionId}Dates") { val startDate = DayDate(seasonCompView.startDate).toString()
                        val endDate = DayDate(seasonCompView.endDate).toString()
                        val join = if (startDate.isNotBlank() || endDate.isNotBlank()) "to" else ""
                        ViewText("$startDate $join $endDate") }

                    item(key = "${seasonCompView.seasonId}-${seasonCompView.competitionId}BlankIcon") {
                        Icon(Blank, "")
                    }

                    if (seasonCompView.competitionType == CompetitionTypes.KNOCK_OUT_CUP.ordinal.toShort()) {
                        item(key = "${seasonCompView.seasonId}-${seasonCompView.competitionId}Rounds") {
                            ClickableIcon(Icons.Default.Rotate90DegreesCcw, "manage season competition rounds") {
                                Editors.SEASON_COMPETITION_ROUND.viewRoute(seasonCompetitionParamOf(seasonCompView))
                            }
                        }
                    } else {
                        item(key = "${seasonCompView.seasonId}-${seasonCompView.competitionId}Teams") {
                            ClickableIcon(Icons.Default.Accessibility, "manage teams") {
                                Editors.SEASON_TEAM_CATEGORY.viewRoute(seasonCompetitionParamOf(seasonCompView))
                            }
                        }
                    }
                    item(key = "${seasonCompView.seasonId}-${seasonCompView.competitionId}Structure") { ClickableIcon(Icons.Default._123, "manage match structure") {
                        Editors.SEASON_TEAMS.viewRoute(seasonCompetitionParamOf(seasonCompView))
                    }}
                }
            }
        })
}

private fun seasonCompetitionParamOf(seasonCompView : SeasonCompView) : SeasonCompetitionParam =
    SeasonCompetitionParam(seasonCompView.seasonId, seasonCompView.seasonName, seasonCompView.competitionId, seasonCompView.competitionName)

@Composable
private fun SeasonEditor(season : Season? = null) {
    val viewModel: SeasonViewModel = koinInject()
    val seasonParameter = parametersOf(season?.id ?: 0)
    val seasonCompetitionState = koinInject<SeasonCompViewModel> { seasonParameter }.getBySeason(season?.id ?: 0).collectAsState(emptyList())
    val competitionState = koinInject<CompetitionViewModel>().uiState.collectAsState(emptyList())
    var name by remember { mutableStateOf(season?.name ?: "") }
    val title = if (season == null) "Add Season" else "Edit Season"
    val startDates = remember { mutableStateMapOf<Short, Int>() }
    val endDates = remember { mutableStateMapOf<Short, Int>() }
    var dirty by remember { mutableStateOf(false) }

    ViewCommon(
        title,
        description = "Return to seasons",
        bottomBar = {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton(OK, Modifier.weight(1f), name.isNotBlank()) {
                    save(season, viewModel, name, competitionState, startDates, endDates)
                    appNavController.popBackStack()
                }
            }
        },
        confirm = { dirty },
        confirmAction = { save(season, viewModel, name, competitionState, startDates, endDates) },
        content = { paddingValues ->
            val competitionList = competitionState.value.sortedBy { it.name.trim().uppercase() }
            if (season != null) {
                for (current in seasonCompetitionState.value) {
                    if (current.startDate > 0) {
                        startDates[current.competitionId] = current.startDate
                    }
                    if (current.endDate > 0) {
                        endDates[current.competitionId] = current.endDate
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText("Name:") }
                item { ViewTextField(value = name, onValueChange = {
                    name = it
                    dirty = checkDirty(season, name, seasonCompetitionState, startDates, endDates)
                }) }
                item { ReadonlyViewText("") }
                item { ReadonlyViewText("Competition") }
                item { ReadonlyViewText("Start") }
                item { ReadonlyViewText("End") }
                for (competition in competitionList) {
                    item { ReadonlyViewText(competition.name) }
                    item {
                        DatePickerView(
                            current = startDates[competition.id] ?: 0,
                            modifier = Modifier,
                            isSelectable = {
                                val dayDate = DayDate(it)
                                dayDate.isMonday() && dayDate.value() < (endDates[competition.id] ?: Integer.MAX_VALUE) }) {
                            startDates[competition.id] = it
                            dirty = checkDirty(season, name, seasonCompetitionState, startDates, endDates)
                        }
                    }
                    item {
                        DatePickerView(
                            current = endDates[competition.id] ?: 0,
                            modifier = Modifier,
                            isSelectable = { val dayDate = DayDate(it)
                                dayDate.isSunday() && dayDate.value() > (startDates[competition.id] ?: 0) }) {
                            endDates[competition.id] = it
                            dirty = checkDirty(season, name, seasonCompetitionState, startDates, endDates)
                        }
                    }
                }
            }
        })
}

private fun checkDirty(season: Season?, name: String, seasonCompetitionState: State<List<SeasonCompView>>, startDates: SnapshotStateMap<CompetitionId, Int>, endDates: SnapshotStateMap<CompetitionId, Int>): Boolean =
    if (season == null) {
        name.isNotEmpty() || startDates.isNotEmpty() || endDates.isNotEmpty()
    } else {
        if (name != season.name) {
            true
        } else {
            var result = false
            for (current in seasonCompetitionState.value) {
                if ((startDates[current.competitionId] ?: 0) != current.startDate ||
                    (endDates[current.competitionId] ?: 0) != current.endDate) {
                    result = true
                    break
                }
            }
            result
        }
    }

private fun save(season : Season?, viewModel: SeasonViewModel, name: String, competitionState: State<List<Competition>>, startDates: SnapshotStateMap<Short, Int>, endDates: SnapshotStateMap<Short, Int>) {
    if (season == null) {
        viewModel.insert(Season(name = name.trim()))
    } else {
        viewModel.update(Season(season.id, name.trim()))
    }
    viewModel.saveCompetitions(name, competitionState, startDates, endDates)
}
