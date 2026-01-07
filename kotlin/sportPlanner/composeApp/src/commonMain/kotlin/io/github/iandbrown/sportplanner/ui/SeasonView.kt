package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled._123
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonDao
import io.github.iandbrown.sportplanner.logic.DayDate
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
    val state = viewModel.uiState.collectAsState()
    val competitionState = koinInject<CompetitionViewModel>().uiState.collectAsState()
    val seasonCompetitionState = koinInject<SeasonCompetitionViewModel>().uiState.collectAsState()

    ViewCommon(MergedState(state.value, competitionState.value, seasonCompetitionState.value),
        navController,
        "Seasons",
        { CreateFloatingAction(navController, editor.addRoute()) },
        content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            val competitionTypeMap = competitionState.value.data?.associateBy({it.id}, {it.type})
            items(
                items = createSeasonsList(state.value.data!!, competitionState.value.data!!, seasonCompetitionState.value.data!!),
                key = { pair -> pair.first }) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        when (val entity = pair.second) {
                            is Season -> {
                                Row(modifier = Modifier.weight(2F), content = {
                                    SpacedViewText(entity.name)
                                })

                                SpacedIcon(Icons.Default.Splitscreen, "manage season breaks") {
                                    navController.navigate(Editors.SEASON_BREAK.viewRoute(entity))
                                }

                                ItemButtons(
                                    editClick = {
                                        navController.navigate(editor.editRoute(entity))
                                    },
                                    deleteClick = { viewModel.delete(entity) })
                            }

                            is SeasonCompetition -> {
                                Row(modifier = Modifier.weight(2F), content = {
                                    Spacer(Modifier.size(32.dp))
                                    SpacedViewText(competitionState.value.data?.first { it.id == entity.competitionId }?.name!!)
                                    SpacedViewText(DayDate(entity.startDate).toString())
                                    SpacedViewText("to")
                                    SpacedViewText(DayDate(entity.endDate).toString())
                                })

                                if (competitionTypeMap?.get(entity.competitionId) == CompetitionTypes.KNOCK_OUT_CUP.ordinal.toShort()) {
                                    SpacedIcon(Icons.Default.Rotate90DegreesCcw, "manage season competition rounds") {
                                        navController.navigate(Editors.SEASON_COMPETITION_ROUND.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                    }
                                }

                                 SpacedIcon(Icons.Default.Accessibility, "manage teams") {
                                    navController.navigate(Editors.SEASON_TEAM_CATEGORY.viewRoute(createSeasonCompetitionParam(state, entity, competitionState.value.data)))
                                }

                                SpacedIcon(Icons.Default._123, "manage match structure") {
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

@Composable
private fun SeasonEditor(navController: NavController, season : Season? = null) {
    val viewModel: SeasonViewModel = koinInject()
    val seasonCompetitionModel: SeasonCompetitionViewModel = koinInject()
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
        { },
        "Return to seasons",
        {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton("ok", Modifier.weight(1f)) {
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
                                seasonCompetitionModel.insert(seasonCompetition)
                            }
                        }
                        navController.popBackStack()
                    }
                }
            }
        }) { paddingValues ->
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
    }
}
