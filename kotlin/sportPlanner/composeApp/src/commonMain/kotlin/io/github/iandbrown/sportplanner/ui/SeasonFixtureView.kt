package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.AssociationName
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.logic.DayDate
import io.github.iandbrown.sportplanner.logic.SeasonLeagueGames
import io.github.iandbrown.sportplanner.logic.SeasonWeeks.Companion.createSeasonWeeks
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import kotlin.time.measureTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

private typealias TeamCountKey = Triple<TeamCategoryId, AssociationName, CompetitionId>
private typealias TeamCountMap = Map<TeamCountKey, Short>

class SeasonFixtureViewModel : ViewModel {
    val database : AppDatabase by inject(AppDatabase::class.java)
    val dao : SeasonFixtureViewDao = database.getSeasonFixtureViewDao()
    private val _uiState = MutableStateFlow(UiState<SeasonFixtureView>(true))
    val uiState = _uiState.asStateFlow()

    constructor(seasonId : Short) {
        viewModelScope.launch {
            flow {
                emit(dao.get(seasonId))
            }.collect {
                _uiState.value = UiState(data = it, isLoading = false)
            }
        }
    }
}

private val editor = Editors.SEASON_FIXTURES

@Composable
fun NavigateFixtures(argument: String?) =
    when {
        argument == null -> {}
        argument.startsWith("Summary") -> {
            val param = argument.substring("Summary&".length)
            SummaryFixtureView(Json.decodeFromString<Season>(param))
        }
        argument.startsWith("View") -> FixtureView()
        else -> FixtureTableView(Json.decodeFromString<Season>(argument))
    }

@Composable
private fun FixtureView() {
    val seasonState by koinInject<SeasonViewModel>().uiState.collectAsState()
    val calculating = remember {mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()

    if (calculating.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon(seasonState, "Fixtures", content = { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(3, 1), Modifier.padding(paddingValues)) {
                item { ViewText("Season") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (season in seasonState.data?.sortedByDescending { it.name.trim().uppercase() }!!) {
                    item { ViewText(season.name) }
                    item { ClickableIcon(Icons.Filled.Summarize, "Show fixture summaries") {
                        "${editor.name}/Summary&${Json.encodeToString(season)}"
                    }}
                    item { ClickableIcon(Icons.Filled.GridView, "Show fixtures") {
                        editor.editRoute(season)
                    }}
                    item { ClickableIconOld(Icons.Filled.Calculate, "Calculate fixtures") {
                        calculating.value = true
                        coroutineScope.launch {
                            val timeTaken = measureTime { calcFixtures(season.id) }
                            println("Fixtures calculated in $timeTaken")
                        }
                        calculating.value = false
                    }}
                }
            }
        })
    }
}

private enum class SumType {HOME_TEAM, AWAY_TEAM}

@Composable
private fun SummaryFixtureView(season: Season) {
    val seasonParameter = parametersOf(season.id)
    val state by koinViewModel<SeasonFixtureViewModel> { parametersOf(season.id) }.uiState.collectAsState()
    val associationState by koinInject<AssociationViewModel>().uiState.collectAsState()
    val competitionState by koinInject<CompetitionViewModel>().uiState.collectAsState()
    val seasonTeamsState by koinInject<SeasonTeamViewModel>{seasonParameter}.uiState.collectAsState()
    val seasonCompetitionState by koinInject<SeasonCompetitionViewModel>{ seasonParameter }.uiState.collectAsState()
    var competitionFilter by remember { mutableStateOf(0.toShort()) }

    ViewCommon(
        MergedState(state, associationState, competitionState, seasonTeamsState, seasonCompetitionState),
        "Season fixture Summary",
        "Return to seasons screen",
        content = {paddingValues ->
            val countsByTeamAndCategory = mutableMapOf<Triple<String, String, SumType>, Int>()
            val associationNameMap = associationState.data?.associateBy({it.id}, {it.name})
            val teamCounts = getTeamCounts(seasonTeamsState, season, associationNameMap)
            val teamCategories = sortedSetOf<String>()
            val teams = sortedSetOf<String>()
            val filteredFixtures = state.data?.filter { it.competitionId == competitionFilter && (it.homeTeamNumber > 0 || it.awayTeamNumber > 0 )}
            for (seasonFixture in filteredFixtures!!) {
                teamCategories += seasonFixture.teamCategoryName
                val homeTeamName = teamName(seasonFixture, true, teamCounts)
                val awayTeamName = teamName(seasonFixture, false, teamCounts)
                teams += homeTeamName
                teams += awayTeamName

                countsByTeamAndCategory.merge(Triple(homeTeamName, seasonFixture.teamCategoryName, SumType.HOME_TEAM), 1, Int::plus)
                countsByTeamAndCategory.merge(Triple(awayTeamName, seasonFixture.teamCategoryName, SumType.AWAY_TEAM), 1, Int::plus)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth().padding(0.dp), content = {
                    val competitionLookup = competitionState.data?.associateBy { it.id }
                    val competitionNameToId = competitionState.data?.associateBy({ it.name }, { it.id })
                    val competitionList = seasonCompetitionState.data?.filter {
                        competitionLookup?.get(it.competitionId)?.type == 0.toShort()
                    }?.map { competitionLookup?.get(it.competitionId)?.name!! }!!
                    val initialCompetition = if (competitionList.isNotEmpty())  competitionNameToId?.get(competitionList[0])!! else 0.toShort()
                    if (competitionFilter != initialCompetition) {
                        competitionFilter = initialCompetition
                    }
                    DropdownList(
                        competitionList,
                        0,
                        label = "Competition"
                    ) { val competitionName = competitionList[it]
                        competitionFilter = competitionNameToId?.get(competitionName)!! }
                })
                LazyVerticalGrid(columns = DoubleFirstGridCells(teamCategories.size)) {
                    item { ViewText("") }
                    for (teamCategory in teamCategories) {
                        item { ViewText(teamCategory) }
                    }
                    for (team in teams) {
                        item { ViewText("$team HOME") }
                        for (teamCategory in teamCategories) {
                            item {
                                val key = Triple(team, teamCategory, SumType.HOME_TEAM)
                                ViewText("${countsByTeamAndCategory[key] ?: 0}")
                            }
                        }
                        item { ViewText("$team AWAY") }
                        for (teamCategory in teamCategories) {
                            item {
                                val key = Triple(team, teamCategory, SumType.AWAY_TEAM)
                                ViewText("${countsByTeamAndCategory[key] ?: 0}")
                            }
                        }
                    }
                }
            }
        })
}

@Composable
private fun FixtureTableView(season: Season) {
    val seasonParameters = parametersOf(season.id)
    val viewModel : SeasonFixtureViewModel = koinViewModel { seasonParameters }
    val associationState by koinInject<AssociationViewModel>().uiState.collectAsState()
    val teamCategoryState by koinInject<TeamCategoryViewModel>().uiState.collectAsState()
    val seasonTeamsState by koinInject<SeasonTeamViewModel> {seasonParameters}.uiState.collectAsState()
    val state = viewModel.uiState.collectAsState()
    val filterAssociation = remember { mutableStateOf("") }
    val filterTeamCategory = remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        MergedState(state.value, associationState, teamCategoryState, seasonTeamsState),
        "Season fixtures",
        "Return to seasons screen",
        {
            BottomBarWithButton("Export") {
                coroutineScope.launch {
                    val associationNameMap =
                        associationState.data?.associateBy({ it.id }, { it.name })
                    val teamCounts = getTeamCounts(seasonTeamsState, season, associationNameMap)
                    val file = FileKit.openFileSaver(suggestedName = "seasonFixtures", extension = "csv")
                    val withTeamCategory = filterTeamCategory.value.isBlank()
                    val matchDayAdjust = teamCategoryState.data?.associateBy({it.name}, { it.matchDay })
                    val sink = file?.sink(append = false)?.buffered()

                    sink.use { bufferedSink ->
                        val fixtures = getFixtures(state.value.data!!, filterAssociation.value, filterTeamCategory.value)
                            .sortedBy { adjustedWeek(it, matchDayAdjust!!) }
                        if (withTeamCategory) {
                            bufferedSink?.writeString("Date,Team Category,Message,Home,Away\n")
                            for (fixture in fixtures) {
                                bufferedSink?.writeString(
                                    "${adjustedDate(fixture, matchDayAdjust!!)},${fixture.teamCategoryName},${fixture.message},${teamName(fixture, true, teamCounts)},${teamName(fixture, false, teamCounts)}\n"
                                )
                            }
                        } else {
                            bufferedSink?.writeString("Date,Message,Home,Away\n")
                            for (fixture in fixtures) {
                                bufferedSink?.writeString("${adjustedDate(fixture, matchDayAdjust!!)},${fixture.message},${teamName(fixture, true, teamCounts)},${teamName(fixture, false, teamCounts)}\n")
                            }
                        }
                    }
                }
            }
        },
        content = { paddingValues ->
            Column(verticalArrangement = Arrangement.Center) {
                Row(modifier = Modifier.fillMaxWidth().padding(paddingValues), content = {
                    val associationList = listOf("") + associationState.data?.map { it.name }!!
                    val teamCategoryList = listOf("") + teamCategoryState.data?.map { it.name }!!
                    val modifier = Modifier.weight(1f)
                    DropdownList(
                        associationList,
                        0,
                        modifier = modifier,
                        label = "Filter Association"
                    ) { filterAssociation.value = associationList[it] }
                    DropdownList(
                        teamCategoryList,
                        0,
                        modifier = modifier,
                        label = "Filter Team Category"
                    ) { filterTeamCategory.value = teamCategoryList[it] }
                })
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    val modifier = Modifier.weight(1f)
                    SpacedViewText("Date", modifier)
                    if (filterTeamCategory.value.isBlank()) {
                        SpacedViewText("Team Category", modifier)
                    }
                    SpacedViewText("Message", modifier)
                    SpacedViewText("Home", modifier)
                    SpacedViewText("Away", modifier)

                })
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth(), content = {
                    val matchDayAdjust = teamCategoryState.data?.associateBy({it.name}, { it.matchDay })
                    val associationNameMap = associationState.data?.associateBy({it.id}, {it.name})
                    val teamCounts = getTeamCounts(seasonTeamsState, season, associationNameMap)
                    val values = getFixtures(state.value.data!!, filterAssociation.value, filterTeamCategory.value)
                        .sortedBy { adjustedWeek(it, matchDayAdjust!!) }

                    items(
                        items = values,
                        key = { seasonFixture -> seasonFixture.id }) { seasonFixture ->
                        Row(modifier = Modifier.fillMaxWidth(), content = {
                            val modifier = Modifier.weight(1f)
                            SpacedViewText(adjustedDate(seasonFixture, matchDayAdjust!!), modifier)
                            if (filterTeamCategory.value.isBlank()) {
                                SpacedViewText(seasonFixture.teamCategoryName, modifier)
                            }
                            SpacedViewText(seasonFixture.message, modifier)
                            SpacedViewText(teamName(seasonFixture, true, teamCounts), modifier)
                            SpacedViewText(teamName(seasonFixture, false, teamCounts), modifier)
                        })
                    }
                })
            }
        })
}

private fun adjustedWeek(fixture: SeasonFixtureView, matchDayAdjust: Map<String, Short>) : Int =
    if (matchDayAdjust[fixture.teamCategoryName]!! > 0.toShort()) {
        DayDate(fixture.date).addDays(matchDayAdjust[fixture.teamCategoryName]!!.toInt()).value()
    } else {
        DayDate(fixture.date).value()
    }

private fun adjustedDate(fixture: SeasonFixtureView, matchDayAdjust: Map<String, Short>): String =
    DayDate(adjustedWeek(fixture, matchDayAdjust)).toString()


private fun getFixtures(allFixtures: List<SeasonFixtureView>, filterAssociation: String, filterTeamCategory: String)
: List<SeasonFixtureView> = allFixtures.sortedBy { it.date }.filter {
    if (filterAssociation.isNotBlank() && it.homeAssociation != filterAssociation && it.awayAssociation != filterAssociation) {
        false
    } else if (filterTeamCategory.isNotBlank() && it.teamCategoryName != filterTeamCategory) {
        false
    } else {
        true
    }
}

private fun getTeamCounts(seasonTeamsState: UiState<SeasonTeam>, season: Season, associationNameMap: Map<Short, String>?): TeamCountMap? =
    seasonTeamsState.data?.
    filter { it.seasonId == season.id }?.
    associateBy({ Triple(it.teamCategoryId, associationNameMap?.get(it.associationId)!!, it.competitionId) }, { it.count })

private fun teamName(fixture: SeasonFixtureView, home : Boolean, teamCountMap : TeamCountMap?) : String {
    val key = if (home) {
        Triple(fixture.teamCategoryId, fixture.homeAssociation, fixture.competitionId)
    } else {
        Triple(fixture.teamCategoryId, fixture.awayAssociation, fixture.competitionId)
    }

    return when (teamCountMap?.get(key)) {
        null -> ""
        0.toShort() -> ""
        1.toShort() -> key.second
        else -> teamName(key.second, fixture.homeTeamNumber)
    }
}

fun teamName(association : String, number : Short) : String {
    val postfix = when (number) {
        0.toShort() -> ""
        1.toShort() -> " A"
        else -> " B"
    }
    return "$association$postfix"
}

private suspend fun calcFixtures(seasonId : Short) {
    val db : AppDatabase by inject(AppDatabase::class.java)
    val seasonFixtureDao = db.getSeasonFixtureDao()
    val seasonWeeks = createSeasonWeeks(seasonId)
    val leagueGames = SeasonLeagueGames()
    val seasonTeamDao = db.getSeasonTeamDao()
    val activeLeagueCompetitions = db.getSeasonCompetitionDao().getActiveLeagueCompetitions(seasonId)

    for (activeLeagueCompetition in activeLeagueCompetitions) {
        for (activeTeamCategory in db.getSeasonTeamCategoryDao().getActiveTeamCategories(seasonId, activeLeagueCompetition.competitionId)) {
            seasonFixtureDao.deleteBySeasonTeamCategory(seasonId, activeTeamCategory.teamCategoryId, activeLeagueCompetition.competitionId)

            if (activeTeamCategory.games == 0.toShort()) {
                continue
            }

            for (seasonBreak in seasonWeeks.breakWeeks()) {
                    seasonFixtureDao.insert(SeasonFixture(0,
                        seasonId,
                        activeLeagueCompetition.competitionId,
                        activeTeamCategory.teamCategoryId,
                        seasonBreak.key,
                        seasonBreak.value,
                        0.toShort(),
                        0.toShort(),
                        0.toShort(),
                        0.toShort()))
            }
            leagueGames.prepareGames(
                activeLeagueCompetition.competitionId,
                activeTeamCategory.teamCategoryId,
                activeTeamCategory.games,
                seasonTeamDao.getTeams(seasonId, activeTeamCategory.competitionId, activeTeamCategory.teamCategoryId))
        }
    }

    val teamsByCategoryAndCompetition = mutableMapOf<Pair<Short, Short>, Int>()
    for (seasonTeam in seasonTeamDao.get(seasonId)) {
        val key = Pair(seasonTeam.teamCategoryId, seasonTeam.competitionId)
        teamsByCategoryAndCompetition[key] = teamsByCategoryAndCompetition.getOrPut(key) { 0 } + seasonTeam.count
    }

    for (fixture in leagueGames.scheduleFixtures(seasonId,
        seasonWeeks,
        db.getTeamCategoryDao().getAll(),
        db.getSeasonTeamCategoryDao().get(seasonId),
        db.getSeasonCompRoundViewDao().getBySeason(seasonId),
        teamsByCategoryAndCompetition,
        activeLeagueCompetitions.map {it.competitionId}.toSet())) {
        seasonFixtureDao.insert(fixture)
    }
}
