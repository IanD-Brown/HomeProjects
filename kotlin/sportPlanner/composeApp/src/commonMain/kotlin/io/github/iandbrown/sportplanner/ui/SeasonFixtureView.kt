package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
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
import com.skydoves.compose.stability.runtime.TraceRecomposition
import io.github.iandbrown.sportplanner.database.AssociationName
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompRoundViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureDao
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonLeagueTeamView
import io.github.iandbrown.sportplanner.database.SeasonLeagueTeamViewDao
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.di.inject
import io.github.iandbrown.sportplanner.logic.DayDate
import io.github.iandbrown.sportplanner.logic.SeasonLeagueGames
import io.github.iandbrown.sportplanner.logic.SeasonWeeks
import io.github.iandbrown.sportplanner.logic.SeasonWeeksImpl.Companion.createSeasonWeeks
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import kotlin.time.measureTime
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private typealias TeamCountKey = Triple<TeamCategoryId, AssociationName, CompetitionId>
private typealias TeamCountMap = Map<TeamCountKey, Short>

class SeasonFixtureViewModel(seasonId: SeasonId) :
    BaseSeasonReadViewModel<SeasonFixtureViewDao, SeasonFixtureView>(
        seasonId,
        inject<SeasonFixtureViewDao>().value
    )

class SeasonLeagueTeamViewModel(seasonId: SeasonId) :
    BaseSeasonReadViewModel<SeasonLeagueTeamViewDao, SeasonLeagueTeamView>(
        seasonId,
        inject<SeasonLeagueTeamViewDao>().value
    )

private val editor = Editors.SEASON_FIXTURES

@Composable
fun NavigateFixtures(argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("Summary") -> {
            val param = argument.substring("Summary&".length)
            SummaryFixtureView(Json.decodeFromString<Season>(param))
        }
        argument.startsWith("View") -> FixtureView()
        argument.startsWith("Date") -> FixtureDateView(Json.decodeFromString<Season>(argument.substring("Date&".length)))
        else -> FixtureTableView(Json.decodeFromString<Season>(argument))
    }
}

@Composable
private fun CompetitionFilter(seasonCompViews : List<SeasonCompView>, selectedCompetitionId: CompetitionId, onClick : (Short) -> Unit) {
    val competitionNameToId = seasonCompViews.associateBy({ it.competitionName }, { it.competitionId })
    val competitionIdToName = seasonCompViews.associateBy ({ it.competitionId }, {it.competitionName})
    val competitionNames = seasonCompViews
        .filter {it.competitionType == CompetitionTypes.LEAGUE.ordinal.toShort()}
        .map { it.competitionName }
    val selectedIndex = competitionNames.indexOf(competitionIdToName[selectedCompetitionId])
    if (competitionNames.isNotEmpty() && selectedIndex < 0) {
        onClick(competitionNameToId[competitionNames[0]]!!)
    }
    DropdownList(
        competitionNames,
        if (selectedIndex >= 0) selectedIndex else 0,
        isLocked = { competitionNames.size == 1 }
    ) {
        onClick(competitionNameToId[competitionNames[it]]!!)
    }
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
        ViewCommon("Fixtures", content = { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(4, 1), Modifier.padding(paddingValues)) {
                item { ViewText("Season") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (season in seasonState.sortedByDescending { it.name.trim().uppercase() }) {
                    item { ViewText(season.name) }
                    item { ClickableIcon(Icons.Filled.Info, "Date summary") {
                        "${editor.name}/Date&${Json.encodeToString(season)}"
                    }}
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

@TraceRecomposition
@Composable
private fun SummaryFixtureView(season: Season) {
    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    val seasonFixtureViewModel = koinInject<SeasonFixtureViewModel> { parametersOf(season.id) }
    val seasonCompetitionState by koinInject<SeasonCompViewModel>().uiState.collectAsState()
    val state = seasonFixtureViewModel.uiState.collectAsState(emptyList())
    val seasonLeagueTeamState by koinInject<SeasonLeagueTeamViewModel> { parametersOf(season.id) }.uiState.collectAsState(emptyList())

    ViewCommon(
        "Season fixture Summary",
        "Return to seasons screen",
        content = {paddingValues ->
            val countsByTeamAndCategory = mutableMapOf<Triple<String, String, SumType>, Int>()
            val teamCounts = seasonLeagueTeamState.associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
            val teamCategories = sortedSetOf<String>()
            val teams = sortedSetOf<String>()
            val filteredFixtures = state.value.filter { it.competitionId == competitionFilter && (it.homeTeamNumber > 0 || it.awayTeamNumber > 0) }
            for (seasonFixture in filteredFixtures) {
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
                    ViewText("Competition", Modifier.align(Alignment.CenterVertically))
                    CompetitionFilter(seasonCompetitionState.filter { it.seasonId == season.id }, competitionFilter) {
                        if (competitionFilter != it) {
                            competitionFilter = it
                        }
                    }
                })
                LazyVerticalGrid(columns = DoubleFirstGridCells(teamCategories.size + 1)) {
                    item { ViewText("") }
                    item { ViewText("") }
                    for (teamCategory in teamCategories) {
                        item { ViewText(teamCategory) }
                    }
                    for (team in teams) {
                        item { ViewText(team) }
                        item { ViewText("HOME") }
                        for (teamCategory in teamCategories) {
                            item {
                                val key = Triple(team, teamCategory, SumType.HOME_TEAM)
                                ViewText("${countsByTeamAndCategory[key] ?: 0}")
                            }
                        }
                        item { ViewText("") }
                        item { ViewText("AWAY") }
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
    val associationState by koinInject<AssociationViewModel>().uiState.collectAsState(emptyList())
    val teamCategoryState by koinInject<TeamCategoryViewModel>().uiState.collectAsState(emptyList())
    val state = viewModel.uiState.collectAsState(emptyList())
    val filterAssociation = remember { mutableStateOf("") }
    val filterTeamCategory = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val withTeamCategory = filterTeamCategory.value.isBlank()
    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    val seasonCompetitionState by koinInject<SeasonCompViewModel>().uiState.collectAsState()
    val seasonLeagueTeamState by koinInject<SeasonLeagueTeamViewModel> { parametersOf(season.id) }.uiState.collectAsState(emptyList())

    ViewCommon(
        "Season fixtures",
        "Return to seasons screen",
        {
            BottomBarWithButton("Export") {
                coroutineScope.launch {
                    val teamCounts = seasonLeagueTeamState.associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
                    val file = FileKit.openFileSaver(suggestedName = "seasonFixtures", extension = "csv")
                    val sink = file?.sink(append = false)?.buffered()

                    sink.use { bufferedSink ->
                        if (bufferedSink != null) {
                            if (withTeamCategory) {
                                bufferedSink.writeString("Date,Team Category,Message,Home,Away\n")
                            }else {
                                bufferedSink.writeString("Date,Message,Home,Away\n")
                            }
                            getFixtures(state.value, competitionFilter, filterAssociation.value, filterTeamCategory.value, teamCategoryState, teamCounts) {
                                    date, teamCategory, message, home, away ->
                                if (withTeamCategory) {
                                    bufferedSink.writeString("$date,$teamCategory,$message,$home,$away\n")
                                } else {
                                    bufferedSink.writeString("$date,$message,$home,$away\n")
                                }
                            }
                        }
                    }
                }
            }
        },
        content = { paddingValues ->
            val columns = if (withTeamCategory) 5 else 4
            val teamCounts = seasonLeagueTeamState.associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
            Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    ViewText("Competition", Modifier.align(Alignment.CenterVertically))
                    CompetitionFilter(
                        seasonCompetitionState.filter { it.seasonId == season.id },
                        competitionFilter
                    ) {
                        if (competitionFilter != it) {
                            competitionFilter = it
                        }
                    }
                })
                Row(modifier = Modifier.fillMaxWidth()) {
                    val associationList = listOf("") + associationState.map { it.name }
                    val teamCategoryList = listOf("") + teamCategoryState.map { it.name }
                    val modifier = Modifier.align(Alignment.CenterVertically).weight(1f)
                    ViewText("Filter Association", modifier)
                    DropdownList(associationList, 0, modifier = modifier) {
                        filterAssociation.value = associationList[it]
                    }
                    ViewText("Filter Team Category", modifier)
                    DropdownList(teamCategoryList, 0, modifier = modifier) {
                        filterTeamCategory.value = teamCategoryList[it]
                    }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
                    item(span = { GridItemSpan(columns) }) {}
                    item(span = { GridItemSpan(columns) }) {
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
                    }
                    getFixtures(state.value, competitionFilter, filterAssociation.value, filterTeamCategory.value, teamCategoryState, teamCounts) {
                            date, teamCategory, message, home, away ->
                        item { ViewText(date) }
                        if (withTeamCategory) {
                            item { ViewText(teamCategory) }
                        }
                        item { ViewText(message) }
                        item { ViewText(home) }
                        item { ViewText(away) }
                    }
                }
            }
        })
}

@Composable
private fun FixtureDateView(season: Season) {
    val seasonParameters = parametersOf(season.id)
    val viewModel : SeasonFixtureViewModel = koinViewModel { seasonParameters }
    val associationState by koinInject<AssociationViewModel>().uiState.collectAsState(emptyList())
    val teamCategoryState by koinInject<TeamCategoryViewModel>().uiState.collectAsState(emptyList())
    val state = viewModel.uiState.collectAsState(emptyList())
    val seasonCompetitionState by koinInject<SeasonCompViewModel>().uiState.collectAsState()
    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    val seasonLeagueTeamState by koinInject<SeasonLeagueTeamViewModel> { parametersOf(season.id) }.uiState.collectAsState(emptyList())

    ViewCommon(
        "Season fixtures",
        "Return to seasons screen") { paddingValues ->
            val columns = associationState.size + 2
            val teamCounts = seasonLeagueTeamState.associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
            val dateList = mutableListOf<String>()
            val dateTotal = mutableMapOf<String, Short>()
            val dateByAssociation = mutableMapOf<String, MutableMap<String, Short>>()
        Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
            Row(modifier = Modifier.fillMaxWidth().padding(0.dp), content = {
                ViewText("Competition", Modifier.align(Alignment.CenterVertically))
                CompetitionFilter(
                    seasonCompetitionState.filter { it.seasonId == season.id },
                    competitionFilter
                ) {
                    if (competitionFilter != it) {
                        competitionFilter = it
                    }
                }
            })
            getFixtures(
                state.value,
                competitionFilter,
                "",
                "",
                teamCategoryState,
                teamCounts
            ) { date, _, _, home, _ ->
                if (dateList.isEmpty() || dateList.last() != date) {
                    dateList.add(date)
                    dateTotal[date] = 0
                    dateByAssociation[date] = mutableMapOf()
                }
                if (home.isNotBlank()) {
                    dateTotal[date] = (dateTotal[date]!! + 1).toShort()
                    if (dateByAssociation[date]?.contains(home) == false) {
                        dateByAssociation[date]?.put(home, 0)
                    }
                    dateByAssociation[date]?.set(home, (dateByAssociation[date]!![home]!! + 1).toShort())
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(columns), Modifier.padding(paddingValues)) {
                item { ReadonlyViewText("Date") }
                item { ReadonlyViewText("Match total") }
                for (association in associationState) {
                    item { ReadonlyViewText("${association.name}(H)") }
                }
                for (date in dateList) {
                    if (dateTotal[date]!! > 0) {
                        item { ReadonlyViewText(date) }
                        item { ReadonlyViewText("${dateTotal[date]}") }
                        for (association in associationState) {
                            item {
                                ReadonlyViewText("${dateByAssociation[date]?.get(association.name) ?: 0}")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun adjustedWeek(fixture: SeasonFixtureView, matchDayAdjust: Map<String, Short>) : Int =
    if (matchDayAdjust[fixture.teamCategoryName]!! > 0.toShort()) {
        DayDate(fixture.date).addDays(matchDayAdjust[fixture.teamCategoryName]!!.toInt()).value()
    } else {
        DayDate(fixture.date).value()
    }

private fun adjustedDate(fixture: SeasonFixtureView, matchDayAdjust: Map<String, Short>): String =
    DayDate(adjustedWeek(fixture, matchDayAdjust)).toString()


private fun getFixtures(
    allFixtures: List<SeasonFixtureView>,
    filterCompetition: CompetitionId,
    filterAssociation: String,
    filterTeamCategory: String,
    teamCategories: List<TeamCategory>,
    teamCounts: TeamCountMap,
    fixtureConsumer: (String, String, String, String, String) -> Unit
) {
    val matchDayAdjust = teamCategories.associateBy({it.name}, { it.matchDay })
    allFixtures.sortedBy { it.date }
        .filter { it.competitionId == filterCompetition }
        .filter {
            if (it.homeAssociation.isBlank() && (filterTeamCategory.isBlank() || it.teamCategoryName == filterTeamCategory)) {
                true
            } else if (filterAssociation.isNotBlank() && it.homeAssociation != filterAssociation && it.awayAssociation != filterAssociation) {
                false
            } else if (filterTeamCategory.isNotBlank() && it.teamCategoryName != filterTeamCategory) {
                false
            } else {
                true
            }
        }
        .sortedBy { adjustedWeek(it, matchDayAdjust) }
        .forEach {
            fixtureConsumer(adjustedDate(it, matchDayAdjust),
                it.teamCategoryName,
                it.message,
                teamName(it, true, teamCounts),
                teamName(it, false, teamCounts))
        }
}

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

internal suspend fun calcFixtures(
    seasonId: SeasonId,
    seasonFixtureDao: SeasonFixtureDao = inject<SeasonFixtureDao>().value,
    seasonTeamDao: SeasonTeamDao = inject<SeasonTeamDao>().value,
    seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>().value,
    seasonTeamCategoryDao: SeasonTeamCategoryDao = inject<SeasonTeamCategoryDao>().value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>().value,
    seasonCompRoundViewDao: SeasonCompRoundViewDao = inject<SeasonCompRoundViewDao>().value,
    seasonWeeks: SeasonWeeks? = null
) {
    val resolvedSeasonWeeks = seasonWeeks ?: createSeasonWeeks(seasonId)
    val leagueGames = SeasonLeagueGames()
    val activeLeagueCompetitions = seasonCompetitionDao.getActiveLeagueCompetitions(seasonId)

    for (activeLeagueCompetition in activeLeagueCompetitions) {
        for (activeTeamCategory in seasonTeamCategoryDao.getActiveTeamCategories(seasonId, activeLeagueCompetition.competitionId)) {
            seasonFixtureDao.deleteBySeasonTeamCategory(seasonId, activeTeamCategory.teamCategoryId, activeLeagueCompetition.competitionId)

            if (activeTeamCategory.games == 0.toShort()) {
                continue
            }

            for (seasonBreak in resolvedSeasonWeeks.breakWeeks()) {
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
                seasonTeamDao.getTeams(seasonId, activeLeagueCompetition.competitionId, activeTeamCategory.teamCategoryId))
        }
    }

    val teamsByCategoryAndCompetition = mutableMapOf<Pair<TeamCategoryId, CompetitionId>, Int>()
    for (seasonTeam in seasonTeamDao.getBySeason(seasonId)) {
        val key = Pair(seasonTeam.teamCategoryId, seasonTeam.competitionId)
        teamsByCategoryAndCompetition[key] = teamsByCategoryAndCompetition.getOrPut(key) { 0 } + seasonTeam.count
    }

    for (fixture in leagueGames.scheduleFixtures(seasonId,
        resolvedSeasonWeeks,
        teamCategoryDao.getAsList(),
        seasonTeamCategoryDao.getBySeasonId(seasonId),
        seasonCompRoundViewDao.getBySeason(seasonId),
        teamsByCategoryAndCompetition,
        activeLeagueCompetitions.map {it.competitionId}.toSet())) {
        seasonFixtureDao.insert(fixture)
    }
}
