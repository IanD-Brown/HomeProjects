package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.AssociationName
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.FarAssociationDao
import io.github.iandbrown.sportplanner.database.FarAssociationView
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompRoundViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureDao
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonLeagueTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonLeagueTeamView
import io.github.iandbrown.sportplanner.database.SeasonLeagueTeamViewDao
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.logic.DayDate
import io.github.iandbrown.sportplanner.logic.SeasonLeagueGames
import io.github.iandbrown.sportplanner.logic.SeasonWeeks
import io.github.iandbrown.sportplanner.logic.SeasonWeeksImpl.Companion.createSeasonWeeks
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.time.measureTime

internal typealias TeamCountKey = Triple<TeamCategoryId, AssociationName, CompetitionId>
internal typealias TeamCountMap = Map<TeamCountKey, Short>

class SeasonFixtureViewModel(seasonId: SeasonId,
                             dao : SeasonFixtureViewDao) :
    BaseSeasonReadViewModel<SeasonFixtureViewDao, SeasonFixtureView>(seasonId, dao)

class SeasonLeagueTeamViewModel(seasonId: SeasonId,
                                dao : SeasonLeagueTeamViewDao) :
    BaseSeasonReadViewModel<SeasonLeagueTeamViewDao, SeasonLeagueTeamView>(seasonId, dao)

class SeasonLeagueTeamCategoryViewModel(seasonId: SeasonId,
                                dao : SeasonLeagueTeamCategoryDao) :
    BaseSeasonReadViewModel<SeasonLeagueTeamCategoryDao, SeasonTeamCategory>(seasonId, dao)

private val editor = Editors.SEASON_LEAGUE_FIXTURES

@Composable
fun NavigateLeagueFixtures(argument: String?) {
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

@Suppress("ParamsComparedByRef")
@Composable
private fun CompetitionFilter(selectedCompetitionId: CompetitionId,
                              seasonId: SeasonId,
                              modifier: Modifier,
                              seasonCompModel : SeasonCompViewModel =  koinViewModel(),
                              onClick : (CompetitionId) -> Unit) {
    val seasonCompetitionState by seasonCompModel.getState().collectAsState()
    val seasonCompViews = seasonCompetitionState
        .values()
        .filter { it.seasonId == seasonId }
        .filter { it.competitionType == CompetitionTypes.LEAGUE.ordinal.toShort() }
    val competitionNameToId = seasonCompViews.associateBy({ it.competitionName }, { it.competitionId })
    val competitionIdToName = seasonCompViews.associateBy ({ it.competitionId }, {it.competitionName})
    val competitionNames = seasonCompViews.map { it.competitionName }.toImmutableList()
    val selectedIndex = competitionNames.indexOf(competitionIdToName[selectedCompetitionId])
    if (competitionNames.isNotEmpty() && selectedIndex < 0) {
        onClick(competitionNameToId[competitionNames[0]]!!)
    }
    Spacer(modifier = Modifier.size(16.dp))
    DropdownList(
        competitionNames,
        if (selectedIndex >= 0) selectedIndex else 0,
        modifier,
        isLocked = { competitionNames.size == 1 }
    ) {
        val filterCompetition = competitionNameToId[competitionNames[it]]!!
        if (filterCompetition != selectedCompetitionId) {
            onClick(filterCompetition)
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun FixtureView(viewModel: SeasonViewModel= koinViewModel()) {
    val seasonState = viewModel.getState().collectAsState()
    val calculating = remember {mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()

    if (calculating.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon("Fixtures",
            states = persistentListOf(seasonState.value)) { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(4, 1), Modifier.padding(paddingValues)) {
                item { ViewText("Season") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (season in seasonState.values().sortedByDescending { it.name.trim().uppercase() }) {
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
        }
    }
}

internal enum class SumType(val displayName: String) {HOME_TEAM("Home"), AWAY_TEAM("Away"), DISTANT("Distant")}

@Suppress("ParamsComparedByRef")
@Composable
private fun SummaryFixtureView(season: Season,
                               seasonFixtureViewModel : SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) },
                               seasonLeagueTeamModel : SeasonLeagueTeamViewModel = koinViewModel { parametersOf(season.id) },
                               seasonTeamCategoryModel : SeasonLeagueTeamCategoryViewModel = koinViewModel { parametersOf(season.id) },
                               farAssociationViewModel: FarAssociationListViewModel = koinViewModel()) {
    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    val state = seasonFixtureViewModel.getState().collectAsState()
    val seasonLeagueTeamState = seasonLeagueTeamModel.getState().collectAsState()
    val seasonTeamCategoryState = seasonTeamCategoryModel.getState().collectAsState()
    val farAssociationState = farAssociationViewModel.getState().collectAsState()
    var typeFilter by remember { mutableStateOf<SumType?>(null) }

    ViewCommon(
        "Season fixture Summary",
        "Return to seasons screen",
        states = persistentListOf(state.value, seasonLeagueTeamState.value, seasonTeamCategoryState.value, farAssociationState.value),
        content = {paddingValues ->
            val fixtureSummaryDetails = FixtureSummaryDetails(seasonLeagueTeamState.values(),
                state
                    .values()
                    .filter { it.competitionId == competitionFilter && (it.homeTeamNumber > 0 || it.awayTeamNumber > 0) },
                seasonTeamCategoryState.values(),
                farAssociationState.values())

            Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth().padding(0.dp), content = {
                    ViewText("Competition", Modifier.align(Alignment.CenterVertically))
                    CompetitionFilter(competitionFilter, season.id, Modifier.align(Alignment.CenterVertically)) {
                        competitionFilter = it
                    }
                    SpacedViewText("Summary Type")
                    val t = listOf("") + SumType.entries.map { it.displayName }
                    DropdownList(t.toImmutableList(), 0, Modifier.align(Alignment.CenterVertically)) {
                        typeFilter = when(it) {
                            0 -> null
                            else -> SumType.entries[it - 1]
                        }
                    }
                })
                val columns = when (typeFilter) {
                    null -> fixtureSummaryDetails.teamCategories.size + 2
                    else -> fixtureSummaryDetails.teamCategories.size + 1
                }
                LazyVerticalGrid(columns = DoubleFirstGridCells(columns)) {
                    viewTextItems(listOf(""))
                    if (typeFilter == null) {
                        viewTextItems(listOf(""))
                    }
                    viewTextItems(fixtureSummaryDetails.teamCategories.toImmutableList())
                    for (team in fixtureSummaryDetails.teams) {
                        fun sumValue(teamCategory: String, sumType: SumType) : String {
                            return fixtureSummaryDetails.countsByTeamAndCategory[Triple(team, teamCategory, sumType)]?.toString() ?: "0"
                        }
                        when (typeFilter) {
                            null -> {
                                viewTextItems(listOf(team, "HOME") +
                                        fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.HOME_TEAM) })
                                viewTextItems(listOf("", "AWAY") +
                                        fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.AWAY_TEAM) })
                                viewTextItems(listOf("", "DISTANT") +
                                        fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.DISTANT) })
                            }
                            SumType.DISTANT -> {
                                viewTextItems(listOf(team) +
                                        fixtureSummaryDetails.teamCategories.map {
                                            "${sumValue(it, SumType.DISTANT)} (${sumValue(it, SumType.AWAY_TEAM)})"
                                        })
                            }
                            else -> {
                                viewTextItems(listOf(team) +
                                        fixtureSummaryDetails.teamCategories.map { sumValue(it, typeFilter!!) })
                            }
                        }
                    }
                }
            }
        })
}

internal class FixtureSummaryDetails {
    val countsByTeamAndCategory = mutableMapOf<Triple<String, String, SumType>, Int>()
    val teamCategories = sortedSetOf<String>()
    val teams = sortedSetOf<String>()

    constructor(seasonLeagueTeams: List<SeasonLeagueTeamView>,
                filteredFixtures: List<SeasonFixtureView>,
                seasonTeamCategories: List<SeasonTeamCategory>,
                farAssociations: List<FarAssociationView>) {
        val teamCounts = seasonLeagueTeams.associateBy(
            { Triple(it.teamCategoryId, it.associationName, it.competitionId) },
            { it.count })
        val singleGameTeamCategories = seasonTeamCategories.filter { it.games == 1.toShort() }
                .map { it.teamCategoryId }.toSet()
        val distantAwayFixtures = farAssociations.groupBy { it.homeAssociationName }
            .mapValues { it.value.map { value -> value.awayAssociationName }.toSet() }

        for (seasonFixture in filteredFixtures) {
            teamCategories += seasonFixture.teamCategoryName
            val homeTeamName = teamName(seasonFixture, true, teamCounts)
            val awayTeamName = teamName(seasonFixture, false, teamCounts)
            teams += homeTeamName
            teams += awayTeamName

            countsByTeamAndCategory.merge(Triple(homeTeamName, seasonFixture.teamCategoryName, SumType.HOME_TEAM), 1, Int::plus)
            countsByTeamAndCategory.merge(Triple(awayTeamName, seasonFixture.teamCategoryName, SumType.AWAY_TEAM), 1, Int::plus)
            if (singleGameTeamCategories.contains(seasonFixture.teamCategoryId)) {
                if (distantAwayFixtures[seasonFixture.homeAssociation]?.contains(seasonFixture.awayAssociation) == true) {
                    countsByTeamAndCategory.merge(Triple(awayTeamName, seasonFixture.teamCategoryName, SumType.DISTANT), 1, Int::plus)
                }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun FixtureTableView(season: Season,
                             associationModel : AssociationViewModel = koinViewModel(),
                             teamCategoryModel : TeamCategoryViewModel = koinViewModel(), ) {
    val viewModel : SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val seasonLeagueTeamModel : SeasonLeagueTeamViewModel = koinViewModel{ parametersOf(season.id) }
    val associationState = associationModel.getState().collectAsState()
    val teamCategoryState = teamCategoryModel.getState().collectAsState()
    val state = viewModel.getState().collectAsState()
    var filterAssociation by remember { mutableStateOf("") }
    var filterTeamCategory by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val withTeamCategory = filterTeamCategory.isBlank()
    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    val seasonLeagueTeamState = seasonLeagueTeamModel.getState().collectAsState()

    ViewCommon(
        "Season fixtures",
        "Return to seasons screen",
        {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "seasonFixtures", "csv") { writer ->
                    val teamCounts = seasonLeagueTeamState.values().associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
                    var df = DataFrame.emptyOf<Any?>()
                    getFixtures(state.values(), competitionFilter, filterAssociation, filterTeamCategory, teamCategoryState.values(), teamCounts) {
                            date, teamCategory, message, home, away ->
                        df = if (withTeamCategory) {
                            df.concat(dataFrameOf("Date", "Team Category", "Message", "Home", "Away")
                                (date, teamCategory, message, home, away))
                        } else {
                            df.concat(dataFrameOf("Date", "Message", "Home", "Away")
                                (date, message, home, away))
                        }
                    }
                    df.writeCsv(writer)
                }
            )
        },
        states = persistentListOf(associationState.value, teamCategoryState.value, state.value, seasonLeagueTeamState.value)) { paddingValues ->
            val columns = if (withTeamCategory) WeightedIconGridCells(0, 1, 1, 3, 2, 2) else WeightedIconGridCells(0, 1, 3, 2, 2)
            val teamCounts = seasonLeagueTeamState.values().associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
            Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    ViewText("Competition", Modifier.align(Alignment.CenterVertically))
                    CompetitionFilter(competitionFilter, season.id, Modifier.align(Alignment.CenterVertically)) {
                        competitionFilter = it
                    }
                })
                Row(modifier = Modifier.fillMaxWidth()) {
                    val associationList = listOf("") + associationState.values().map { it.name }.sorted()
                    val teamCategoryList = listOf("") + teamCategoryState.values().map { it.name }.sorted()
                    val modifier = Modifier.align(Alignment.CenterVertically).weight(1f)
                    ReadonlyViewText("Filter Team Category", modifier)
                    DropdownList(teamCategoryList.toImmutableList(), teamCategoryList.indexOf(filterTeamCategory), modifier = modifier) {
                        filterTeamCategory = teamCategoryList[it]
                    }
                    ReadonlyViewText("Filter Association", modifier)
                    DropdownList(associationList.toImmutableList(), associationList.indexOf(filterAssociation), modifier = modifier) {
                        filterAssociation = associationList[it]
                    }
                }
                LazyVerticalGrid(columns = columns) {
                    item(span = { GridItemSpan(columns.columnCount) }) {
                        Row(modifier = Modifier.fillMaxWidth(), content = {
                            val modifier = Modifier.weight(1f)
                            SpacedViewText("Date", modifier)
                            if (filterTeamCategory.isBlank()) {
                                SpacedViewText("Team Category", modifier)
                            }
                            SpacedViewText("Message", modifier)
                            SpacedViewText("Home", modifier)
                            SpacedViewText("Away", modifier)
                        })
                    }
                    getFixtures(state.values(), competitionFilter, filterAssociation, filterTeamCategory, teamCategoryState.values(), teamCounts) {
                            date, teamCategory, message, home, away ->
                        item { ViewText(date) }
                        if (withTeamCategory) {
                            item { ViewText(teamCategory) }
                        }
                        viewTextItems(listOf(message, home, away))
                    }
                }
            }
        }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun FixtureDateView(season: Season,
                            viewModel : SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) },
                            associationViewModel : AssociationViewModel = koinViewModel(),
                            teamCategoryViewModel : TeamCategoryViewModel = koinViewModel(),
                            seasonLeagueTeamModel : SeasonLeagueTeamViewModel = koinViewModel { parametersOf(season.id) }) {
    val associationState = associationViewModel.getState().collectAsState()
    val teamCategoryState = teamCategoryViewModel.getState().collectAsState()
    val state = viewModel.getState().collectAsState()
    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    val seasonLeagueTeamState by seasonLeagueTeamModel.getState().collectAsState()

    ViewCommon(
        "Season fixtures",
        "Return to seasons screen",
        states = persistentListOf(associationState.value, teamCategoryState.value)) { paddingValues ->
            val columns = associationState.values().size + 2
            val teamCounts = seasonLeagueTeamState.values().associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
            val dateList = mutableListOf<String>()
            val dateTotal = mutableMapOf<String, Short>()
            val dateByAssociation = mutableMapOf<String, MutableMap<String, Short>>()
        Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
            Row(modifier = Modifier.fillMaxWidth().padding(0.dp), content = {
                ViewText("Competition", Modifier.align(Alignment.CenterVertically))
                CompetitionFilter(competitionFilter, season.id, Modifier.align(Alignment.CenterVertically)) {
                    competitionFilter = it
                }
            })
            getFixtures(
                state.values(),
                competitionFilter,
                "",
                "",
                teamCategoryState.values(),
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
                for (association in associationState.values()) {
                    item { ReadonlyViewText("${association.name}(H)") }
                }
                for (date in dateList) {
                    if (dateTotal[date]!! > 0) {
                        item { ReadonlyViewText(date) }
                        item { ReadonlyViewText("${dateTotal[date]}") }
                        for (association in associationState.values()) {
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


internal fun getFixtures(
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

internal fun teamName(fixture: SeasonFixtureView, home : Boolean, teamCountMap : TeamCountMap?) : String {
    val key = if (home) {
        Triple(fixture.teamCategoryId, fixture.homeAssociation, fixture.competitionId)
    } else {
        Triple(fixture.teamCategoryId, fixture.awayAssociation, fixture.competitionId)
    }

    return when (teamCountMap?.get(key)) {
        null -> ""
        0.toShort() -> ""
        1.toShort() -> key.second
        else -> teamName(key.second, if (home) fixture.homeTeamNumber else fixture.awayTeamNumber)
    }
}

internal fun teamName(association : String, number : Short) : String {
    val postfix = when (number) {
        0.toShort() -> ""
        1.toShort() -> " A"
        else -> " B"
    }
    return "$association$postfix"
}

internal suspend fun calcFixtures(
    seasonId: SeasonId,
    seasonFixtureDao: SeasonFixtureDao = inject<SeasonFixtureDao>(SeasonFixtureDao::class.java).value,
    seasonTeamDao: SeasonTeamDao = inject<SeasonTeamDao>(SeasonTeamDao::class.java).value,
    seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>(SeasonCompetitionDao::class.java).value,
    seasonTeamCategoryDao: SeasonTeamCategoryDao = inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>(TeamCategoryDao::class.java).value,
    seasonCompRoundViewDao: SeasonCompRoundViewDao = inject<SeasonCompRoundViewDao>(SeasonCompRoundViewDao::class.java).value,
    farAssociationDao: FarAssociationDao = inject<FarAssociationDao>(FarAssociationDao::class.java).value,
    seasonWeeks: SeasonWeeks? = null
) {
    val resolvedSeasonWeeks = seasonWeeks ?: createSeasonWeeks(seasonId)
    val leagueGames = SeasonLeagueGames()
    val activeLeagueCompetitions = seasonCompetitionDao.getActiveLeagueCompetitions(seasonId)
    val farAwayGames = farAssociationDao.get()
        .groupBy({ it.awayAssociation}, {it.homeAssociation})
        .mapValues { (_, values) -> values.toSet() }
    val currentHomeFixtureCount = mutableMapOf<Pair<Int,AssociationId>, Int>()
    val matchDayAdjust = teamCategoryDao.get().associateBy({it.id}, { it.matchDay })

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
                seasonTeamDao.getTeams(seasonId, activeLeagueCompetition.competitionId, activeTeamCategory.teamCategoryId),
                farAwayGames)
        }
        seasonFixtureDao.get(seasonId, activeLeagueCompetition.competitionId)
            .groupBy { Pair(DayDate(it.date).addDays(matchDayAdjust[it.teamCategoryId]?.toInt() ?: 0).value(), it.homeAssociationId) }
            .mapValues { (id, list) -> currentHomeFixtureCount.merge(id, list.size, Int::plus) }
    }

    val teamsByCategoryAndCompetition = mutableMapOf<Pair<TeamCategoryId, CompetitionId>, Int>()
    for (seasonTeam in seasonTeamDao.getBySeason(seasonId)) {
        val key = Pair(seasonTeam.teamCategoryId, seasonTeam.competitionId)
        teamsByCategoryAndCompetition.merge(key, 0, Int::plus)
        teamsByCategoryAndCompetition[key] = teamsByCategoryAndCompetition.getOrPut(key) { 0 } + seasonTeam.count
    }

    for (fixture in leagueGames.scheduleFixtures(seasonId,
        resolvedSeasonWeeks,
        teamCategoryDao.get(),
        seasonTeamCategoryDao.getBySeasonId(seasonId),
        seasonCompRoundViewDao.get(seasonId),
        teamsByCategoryAndCompetition,
        activeLeagueCompetitions.map {it.competitionId}.toSet(),
        currentHomeFixtureCount)) {
        seasonFixtureDao.insert(fixture)
    }
}
