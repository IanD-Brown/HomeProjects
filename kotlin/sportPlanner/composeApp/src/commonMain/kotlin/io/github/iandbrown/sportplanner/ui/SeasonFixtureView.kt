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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonTeam
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
fun NavigateFixtures(navController: NavController, argument : String?) =
    when {
        argument == null -> {}
        argument.startsWith("Summary") -> {
            val param = argument.substring("Summary&".length)
            SummaryFixtureView(navController, Json.decodeFromString<Season>(param))
        }
        argument.startsWith("View") -> FixtureView(navController)
        else -> FixtureTableView(navController, Json.decodeFromString<Season>(argument))
    }

@Composable
private fun FixtureView(navController: NavController) {
    val seasonState by koinInject<SeasonViewModel>().uiState.collectAsState()
    val calculating = remember {mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()

    if (calculating.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon(seasonState, navController, "Fixtures", {})
        { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(
                    items = seasonState.data?.sortedByDescending { it.name.trim().uppercase() }!!,
                    key = { it.id }) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            ViewText(it.name)
                        }
                        SpacedIcon(Icons.Filled.Summarize, "Show fixture summaries") {
                            navController.navigate("${editor.name}/Summary&${Json.encodeToString(it)}")
                        }
                        SpacedIcon(Icons.Filled.GridView, "Show fixtures") {
                            navController.navigate(editor.editRoute(it))
                        }
                        SpacedIcon(Icons.Filled.Calculate, "Calculate fixtures") {
                            calculating.value = true
                            coroutineScope.launch {
                                val timeTaken = measureTime { calcFixtures(it.id) }
                                println("Fixtures calculated in $timeTaken")
                            }
                            calculating.value = false
                        }
                    }
                }
            }
        }
    }
}

private enum class SumType {HOME_TEAM, AWAY_TEAM}

@Composable
private fun SummaryFixtureView(navController : NavController, season : Season) {
    val state by koinViewModel<SeasonFixtureViewModel> { parametersOf(season.id) }.uiState.collectAsState()
    val associationState by koinInject<AssociationViewModel>().uiState.collectAsState()
    val competitionState by koinInject<CompetitionViewModel>().uiState.collectAsState()
    val seasonTeamsState by koinInject<SeasonTeamViewModel>().uiState.collectAsState()
    val seasonCompetitionState by koinInject<SeasonCompetitionViewModel>().uiState.collectAsState()
    val competitionFilter = remember { mutableStateOf(0.toShort()) }

    ViewCommon(MergedState(state, associationState, competitionState, seasonTeamsState, seasonCompetitionState),
        navController,
        "Season fixture Summary",
        { },
        "Return to seasons screen", content = {paddingValues ->
            val countsByTeamAndCategory = mutableMapOf<Triple<String, String, SumType>, Int>()
            val associationNameMap = associationState.data?.associateBy({it.id}, {it.name})
            val teamCounts = getTeamCounts(seasonTeamsState, season, associationNameMap)
            val teamCategories = sortedSetOf<String>()
            val teams = sortedSetOf<String>()
            val filteredFixtures = state.data?.filter { it.competitionId == competitionFilter.value && it.homeTeamNumber > 0 || it.awayTeamNumber > 0 }
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
                    val competitionNameToId =
                        competitionState.data?.associateBy({ it.name }, { it.id })
                    val competitionList = seasonCompetitionState.data?.filter {
                        it.seasonId == season.id && competitionLookup?.get(it.competitionId)?.type == 0.toShort()
                    }?.map { competitionLookup?.get(it.competitionId)?.name!! }!!
                    val initialCompetition = competitionNameToId?.get(competitionList[0])!!
                    if (competitionFilter.value != initialCompetition) {
                        competitionFilter.value = initialCompetition
                    }
                    DropdownList(
                        competitionList,
                        0,
                        label = "Competition"
                    ) { competitionFilter.value = competitionNameToId[competitionList[it]]!! }
                })
                LazyVerticalGrid(
                    columns = object : GridCells {
                        override fun Density.calculateCrossAxisCellSizes(
                            availableSize: Int,
                            spacing: Int
                        ): List<Int> {
                            // Define the total available width after accounting for spacing
                            val columnCount = teamCategories.size + 1
                            val totalSpacing = spacing * columnCount
                            val usableWidth = availableSize - totalSpacing

                            // Calculate widths based on a 2:1 ratio (first column twice as wide as second)
                            val firstColumnWidth = (usableWidth * 2 / columnCount)
                            val laterColumnWidth = (usableWidth * 1 / columnCount)
                            val sizes = mutableListOf<Int>().apply {
                                repeat(teamCategories.size) {
                                    add(laterColumnWidth)
                                }
                            }
                            sizes.add(0, firstColumnWidth)

                            return sizes
                        }
                    },
                ) {
                    item { ViewText("") }
                    for (teamCategory in teamCategories) {
                        item { ViewText(teamCategory) }
                    }
                    for (team in teams) {
                        item { ViewText("$team HOME") }
                        for (teamCategory in teamCategories) {
                            item {
                                ViewText(
                                    "${
                                        countsByTeamAndCategory[Triple(
                                            team,
                                            teamCategory,
                                            SumType.HOME_TEAM
                                        )]
                                    }"
                                )
                            }
                        }
                        item { ViewText("$team AWAY") }
                        for (teamCategory in teamCategories) {
                            item {
                                ViewText(
                                    "${
                                        countsByTeamAndCategory[Triple(
                                            team,
                                            teamCategory,
                                            SumType.AWAY_TEAM
                                        )]
                                    }"
                                )
                            }
                        }
                    }
                }
            }
        })
}

@Composable
private fun FixtureTableView(navController : NavController, season : Season) {
    val viewModel : SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val associationState by koinInject<AssociationViewModel>().uiState.collectAsState()
    val teamCategoryState by koinInject<TeamCategoryViewModel>().uiState.collectAsState()
    val seasonTeamsState by koinInject<SeasonTeamViewModel>().uiState.collectAsState()
    val state = viewModel.uiState.collectAsState()
    val filterAssociation = remember { mutableStateOf("") }
    val filterTeamCategory = remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(MergedState(state.value, associationState, teamCategoryState, seasonTeamsState),
        navController,
        "Season fixtures",
        {
            FloatingActionButton(onClick = {
                coroutineScope.launch {
                    val associationNameMap =
                        associationState.data?.associateBy({ it.id }, { it.name })
                    val teamCounts = getTeamCounts(seasonTeamsState, season, associationNameMap)
                    val file = FileKit.openFileSaver(suggestedName = "seasonFixtures", extension = "csv")
                    val withTeamCategory = filterTeamCategory.value.isBlank()

                    val sink = file?.sink(append = false)?.buffered()

                    sink.use { bufferedSink ->
                        if (withTeamCategory) {
                            bufferedSink?.writeString("Date,Team Category,Message,Home,Away\n")
                            for (fixture in getFixtures(state.value.data!!, filterAssociation.value, filterTeamCategory.value)) {
                                bufferedSink?.writeString(
                                    "${DayDate(fixture.date)},${fixture.teamCategoryName},${fixture.message},${teamName(fixture, true, teamCounts)},${teamName(fixture, false, teamCounts)}\n"
                                )
                            }
                        } else {
                            bufferedSink?.writeString("Date,Message,Home,Away\n")
                            for (fixture in getFixtures(state.value.data!!, filterAssociation.value, filterTeamCategory.value)) {
                                bufferedSink?.writeString("${DayDate(fixture.date)},${fixture.message},${teamName(fixture, true, teamCounts)},${teamName(fixture, false, teamCounts)}\n")
                            }
                        }
                    }
                }
            }, content = {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "export", tint = Color.White)
            })
        },
        "Return to seasons screen",
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
                    val associationNameMap = associationState.data?.associateBy({it.id}, {it.name})
                    val teamCounts = getTeamCounts(seasonTeamsState, season, associationNameMap)
                    val values = getFixtures(state.value.data!!, filterAssociation.value, filterTeamCategory.value)
                    items(
                        items = values,
                        key = { seasonFixture -> seasonFixture.id }) { seasonFixture ->
                        Row(modifier = Modifier.fillMaxWidth(), content = {
                            val modifier = Modifier.weight(1f)
                            SpacedViewText(DayDate(seasonFixture.date).toString(), modifier)
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

private fun getTeamCounts(seasonTeamsState: UiState<SeasonTeam>, season: Season, associationNameMap: Map<Short, String>?): Map<Triple<Short, String?, Short>, Short>? =
    seasonTeamsState.data?.
    filter { it.seasonId == season.id }?.
    associateBy({ Triple(it.teamCategoryId, associationNameMap?.get(it.associationId), it.competitionId) }, { it.count })

private fun teamName(fixture: SeasonFixtureView, home : Boolean, teamCount : Map<Triple<Short, String?, Short>, Short>?) : String {
    val key = if (home) {
        Triple(fixture.seasonId, fixture.homeAssociation, fixture.competitionId)
    } else {
        Triple(fixture.seasonId, fixture.awayAssociation, fixture.competitionId)
    }

    return when (teamCount?.get(key)) {
        null -> ""
        0.toShort() -> ""
        1.toShort() -> key.second
        else -> teamName(key.second, fixture.homeTeamNumber)
    }
}

private fun teamName(association : String, number : Short) : String {
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
    for (activeLeagueCompetition in db.getSeasonCompetitionDao().   getActiveLeagueCompetitions(seasonId)) {
        for (activeTeamCategory in db.getSeasonTeamCategoryDao().getActiveTeamCategories(seasonId, activeLeagueCompetition.competitionId)) {
            seasonFixtureDao.deleteBySeasonTeamCategory(seasonId, activeTeamCategory.teamCategoryId, activeLeagueCompetition.competitionId)

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

    for (fixture in leagueGames.scheduleFixtures(seasonId,
        seasonWeeks,
        db.getTeamCategoryDao().getAll(),
        db.getSeasonTeamCategoryDao().getBySeason(seasonId),
        db.getSeasonCompRoundViewDao().getBySeason(seasonId))) {
        seasonFixtureDao.insert(fixture)
    }
}
