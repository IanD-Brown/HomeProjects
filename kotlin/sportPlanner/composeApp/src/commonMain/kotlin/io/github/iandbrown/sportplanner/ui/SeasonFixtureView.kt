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
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.Association
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
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.io.path.Path
import kotlin.time.measureTime

internal typealias TeamCountKey = Triple<TeamCategoryId, AssociationName, CompetitionId>
internal typealias TeamCountMap = Map<TeamCountKey, Short>

class SeasonFixtureViewModel(
    seasonId: SeasonId,
    dao: SeasonFixtureViewDao
) :
    BaseReadViewModel<SeasonFixtureViewDao, SeasonFixtureView>(dao, { it.get(seasonId) })

class SeasonLeagueTeamViewModel(
    seasonId: SeasonId,
    dao: SeasonLeagueTeamViewDao
) :
    BaseReadViewModel<SeasonLeagueTeamViewDao, SeasonLeagueTeamView>(dao, { it.get(seasonId) })

class SeasonLeagueTeamCategoryViewModel(
    seasonId: SeasonId,
    dao: SeasonLeagueTeamCategoryDao
) :
    BaseReadViewModel<SeasonLeagueTeamCategoryDao, SeasonTeamCategory>(dao, { it.get(seasonId) })

@Suppress("ParamsComparedByRef")
@Composable
private fun CompetitionFilter(
    selectedCompetitionId: CompetitionId,
    seasonId: SeasonId,
    modifier: Modifier,
    onClick: (CompetitionId) -> Unit
) {
    val seasonCompModel: SeasonCompViewModel = koinViewModel()
    val seasonCompetitionState by seasonCompModel.getState().collectAsStateWithLifecycle()
    val seasonCompViews = seasonCompetitionState
        .values()
        .filter { it.seasonId == seasonId }
        .filter { it.competitionType == CompetitionTypes.LEAGUE.ordinal.toShort() }
    val competitionNameToId = seasonCompViews.associateBy({ it.competitionName }, { it.competitionId })
    val competitionIdToName = seasonCompViews.associateBy({ it.competitionId }, { it.competitionName })
    val competitionNames = seasonCompViews.map { it.competitionName }.toImmutableList()
    val selectedIndex = competitionNames.indexOf(competitionIdToName[selectedCompetitionId])
    if (competitionNames.isNotEmpty() && (selectedIndex < 0)) {
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

@Composable
fun FixtureScreen() {
    val viewModel: SeasonViewModel = koinViewModel()
    val seasonState by viewModel.getState().collectAsStateWithLifecycle()
    var calculating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (calculating) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        val onSurface = MaterialTheme.colorScheme.onSurface

        ViewCommon("Fixtures",
            states = persistentListOf(seasonState)) { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(4, 1), Modifier.padding(paddingValues)) {
                item { ViewText("Season") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (season in seasonState.values().sortedByDescending { it.name.trim().uppercase() }) {
                    item { ViewText(season.name) }
                    clickableIcon(Icons.Filled.Info, "Date summary", onSurface) {
                        appNavigator.navigate(Route.LeagueFixturesDate(season))
                    }

                    clickableIcon(Icons.Filled.Summarize, "Show fixture summaries", onSurface) {
                        appNavigator.navigate(Route.LeagueFixturesSummary(season))
                    }

                    clickableIcon(Icons.Filled.GridView, "Show fixtures", onSurface) {
                        appNavigator.navigate(Route.LeagueFixturesTable(season))
                    }

                    clickableIcon(Icons.Filled.Calculate, "Calculate fixtures", onSurface) {
                        calculating = true
                        coroutineScope.launch {
                            val timeTaken = measureTime { calcFixtures(season.id) }
                            println("Fixtures calculated in $timeTaken")

                            calculating = false
                        }
                    }
                }
            }
        }
    }
}

internal enum class SumType(val displayName: String) { HOME_TEAM("Home"), AWAY_TEAM("Away"), DISTANT("Distant") }

@Composable
fun SummaryFixtureScreen(season: Season) {
    val seasonFixtureViewModel: SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val seasonLeagueTeamModel: SeasonLeagueTeamViewModel = koinViewModel { parametersOf(season.id) }
    val seasonTeamCategoryModel: SeasonLeagueTeamCategoryViewModel = koinViewModel { parametersOf(season.id) }
    val farAssociationViewModel: FarAssociationListViewModel = koinViewModel()

    val state by seasonFixtureViewModel.getState().collectAsStateWithLifecycle()
    val seasonLeagueTeamState by seasonLeagueTeamModel.getState().collectAsStateWithLifecycle()
    val seasonTeamCategoryState by seasonTeamCategoryModel.getState().collectAsStateWithLifecycle()
    val farAssociationState by farAssociationViewModel.getState().collectAsStateWithLifecycle()

    var competitionFilter by remember { mutableStateOf(0.toShort()) }
    var typeFilter by remember { mutableIntStateOf(0) }

    ViewCommon(
        "Season fixture Summary",
        "Return to seasons screen",
        states = persistentListOf(state, seasonLeagueTeamState, seasonTeamCategoryState, farAssociationState)) { paddingValues ->
            val fixtureSummaryDetails = FixtureSummaryDetails(
                seasonLeagueTeamState.values(),
                state
                    .values()
                    .filter { it.competitionId == competitionFilter && (it.homeTeamNumber > 0 || it.awayTeamNumber > 0) },
                seasonTeamCategoryState.values(),
                farAssociationState.values()
            )

            Column(modifier = Modifier.fillMaxWidth().padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth().padding(0.dp), content = {
                    val modifier = Modifier.align(Alignment.CenterVertically)
                    ViewText("Competition", modifier)
                    CompetitionFilter(competitionFilter, season.id, modifier) {
                        competitionFilter = it
                    }
                    SpacedViewText("Summary Type", modifier)
                    val t = (listOf("") + SumType.entries.map { it.displayName }).toImmutableList()
                    DropdownList(t, typeFilter, modifier) {
                        typeFilter = it
                    }
                })
                val columns = when (typeFilter) {
                    0 -> fixtureSummaryDetails.teamCategories.size + 2
                    else -> fixtureSummaryDetails.teamCategories.size + 1
                }
                LazyVerticalGrid(columns = DoubleFirstGridCells(columns)) {
                    viewTextItems(listOf(""))
                    if (typeFilter == 0) {
                        viewTextItems(listOf(""))
                    }
                    viewTextItems(fixtureSummaryDetails.teamCategories.toImmutableList())
                    for (team in fixtureSummaryDetails.teams) {
                        fun sumValue(teamCategory: String, sumType: SumType): String =
                            fixtureSummaryDetails.countsByTeamAndCategory[Triple(
                                team,
                                teamCategory,
                                sumType
                            )]?.toString() ?: "0"

                        when (typeFilter) {
                            0 -> {
                                viewTextItems(
                                    listOf(team, "HOME") +
                                            fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.HOME_TEAM) }
                                )
                                viewTextItems(
                                    listOf("", "AWAY") +
                                            fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.AWAY_TEAM) }
                                )
                                viewTextItems(
                                    listOf("", "DISTANT") +
                                            fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.DISTANT) }
                                )
                            }

                            SumType.DISTANT.ordinal + 1 -> {
                                if (team in fixtureSummaryDetails.teamsWithDistantGames) {
                                viewTextItems(
                                    listOf(team) +
                                            fixtureSummaryDetails.teamCategories.map {
                                                "${sumValue(it, SumType.DISTANT)} (${sumValue(it, SumType.AWAY_TEAM)})"
                                            }
                                )
                                }
                            }

                            else -> {
                                viewTextItems(
                                    listOf(team) +
                                            fixtureSummaryDetails.teamCategories.map { sumValue(it, SumType.entries[typeFilter - 1]) }
                                )
                            }
                        }
                    }
                }
            }
        }
}

internal class FixtureSummaryDetails {
    val countsByTeamAndCategory = mutableMapOf<Triple<String, String, SumType>, Int>()
    val teamCategories = sortedSetOf<String>()
    val teams = sortedSetOf<String>()
    val teamsWithDistantGames: Set<String>

    constructor(
        seasonLeagueTeams: List<SeasonLeagueTeamView>,
        filteredFixtures: List<SeasonFixtureView>,
        seasonTeamCategories: List<SeasonTeamCategory>,
        farAssociations: List<FarAssociationView>
    ) {
        val teamCounts = seasonLeagueTeams.associateBy(
            { Triple(it.teamCategoryId, it.associationName, it.competitionId) },
            { it.count })
        val singleGameTeamCategories = seasonTeamCategories.filter { it.games == 1.toShort() }
            .map { it.teamCategoryId }.toSet()
        val distantAwayFixtures = farAssociations.groupBy { it.homeAssociationName }
            .mapValues { it.value.map { value -> value.awayAssociationName }.toSet() }
        val hasDistantGame = mutableSetOf<String>()

        for (seasonFixture in filteredFixtures) {
            teamCategories += seasonFixture.teamCategoryName
            val homeTeamName = teamName(seasonFixture, true, teamCounts)
            val awayTeamName = teamName(seasonFixture, false, teamCounts)
            teams += homeTeamName
            teams += awayTeamName

            countsByTeamAndCategory.merge(
                Triple(homeTeamName, seasonFixture.teamCategoryName, SumType.HOME_TEAM),
                1,
                Int::plus
            )
            countsByTeamAndCategory.merge(
                Triple(awayTeamName, seasonFixture.teamCategoryName, SumType.AWAY_TEAM),
                1,
                Int::plus
            )
            if (singleGameTeamCategories.contains(seasonFixture.teamCategoryId)) {
                if (distantAwayFixtures[seasonFixture.homeAssociation]?.contains(seasonFixture.awayAssociation) == true) {
                    countsByTeamAndCategory.merge(
                        Triple(awayTeamName, seasonFixture.teamCategoryName, SumType.DISTANT),
                        1,
                        Int::plus
                    )
                    hasDistantGame += awayTeamName
                }
            }
        }
        teamsWithDistantGames = hasDistantGame
    }
}

@Composable
fun FixtureTableScreen(season: Season) {
    val associationModel: AssociationViewModel = koinViewModel()
    val teamCategoryModel: TeamCategoryViewModel = koinViewModel()
    val viewModel: SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val seasonLeagueTeamModel: SeasonLeagueTeamViewModel = koinViewModel { parametersOf(season.id) }

    val associationState by associationModel.getState().collectAsStateWithLifecycle()
    val teamCategoryState by teamCategoryModel.getState().collectAsStateWithLifecycle()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val seasonLeagueTeamState by seasonLeagueTeamModel.getState().collectAsStateWithLifecycle()

    var filterAssociation by remember { mutableStateOf("") }
    var filterTeamCategory by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val withTeamCategory = filterTeamCategory.isBlank()
    var competitionFilter by remember { mutableStateOf(0.toShort()) }

    fun getSourceFixtureValues() = SourceFixtureValues(state.values(),
        teamCategoryState.values(), seasonLeagueTeamState.values()
            .associateBy(
                { Triple(it.teamCategoryId, it.associationName, it.competitionId) },
                { it.count }))

    ViewCommon(
        "Season fixtures",
        "Return to seasons screen",
        {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "seasonFixtures", "csv") { writer ->
                    export(getSourceFixtureValues(),
                        FixtureFilter(competitionFilter, filterAssociation, filterTeamCategory),
                        withTeamCategory,
                        writer
                    )
                },
                ButtonSettings(imageVector = Icons.Default.MultipleStop) {
                    coroutineScope.launch {
                        val folder = FileKit.openDirectoryPicker()
                        if (folder != null) {
                            val sourceFixtureValues = getSourceFixtureValues()

                            for (saturday in listOf(true, false)) {
                                crossTeamCategoryAssociationExport(
                                    saturday,
                                    folder,
                                    sourceFixtureValues,
                                    associationState.values(),
                                    competitionFilter
                                )
                            }

                            teamCategoryState.values().forEach {
                                val teamCategoryName = it.name
                                val fixtureFilter =
                                    FixtureFilter(competitionFilter, "", teamCategoryName)
                                teamCategoryExport(
                                    it.matchDay == Day.SAT.ordinal.toShort(),
                                    folder,
                                    teamCategoryName,
                                    sourceFixtureValues,
                                    fixtureFilter
                                )
                            }
                        }
                    }
                },
                ButtonSettings(imageVector = Icons.Default.Web) {
                    coroutineScope.launch {
                        val folder = FileKit.openDirectoryPicker()
                        if (folder != null) {
                            val sourceFixtureValues = getSourceFixtureValues()

                            teamCategoryState.values().forEach {
                                val teamCategoryName = it.name
                                val fixtureFilter =
                                    FixtureFilter(competitionFilter, "", teamCategoryName)
                                teamCategoryExportForLeagueRepublic(
                                    it.matchDay == Day.SAT.ordinal.toShort(),
                                    folder,
                                    teamCategoryName,
                                    sourceFixtureValues,
                                    fixtureFilter
                                )
                            }
                        }
                    }
                },
                ButtonSettings(enabled = filterAssociation.isNotBlank(), imageVector = Icons.Default.CalendarViewMonth) {
                    coroutineScope.launch {
                        val folder = FileKit.openDirectoryPicker()
                        val sourceFixtureValues = getSourceFixtureValues()

                        if (folder != null) {
                            teamCategoryState.values().forEach {
                                val teamCategoryName = it.name
                                val fixtureFilter =
                                    FixtureFilter(competitionFilter, filterAssociation, teamCategoryName)
                                spondExport(
                                    it.matchDay == Day.SAT.ordinal.toShort(),
                                    folder,
                                    teamCategoryName,
                                    sourceFixtureValues,
                                    fixtureFilter
                                )
                            }
                        }
                    }
                }
            )
        },
        states = persistentListOf(associationState, teamCategoryState, state, seasonLeagueTeamState)
    ) { paddingValues ->
        val columns =
            if (withTeamCategory) WeightedIconGridCells(0, 1, 1, 3, 2, 2) else WeightedIconGridCells(0, 1, 3, 2, 2)
        val teamCounts = seasonLeagueTeamState.values()
            .associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
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
                DropdownList(
                    teamCategoryList.toImmutableList(),
                    teamCategoryList.indexOf(filterTeamCategory),
                    modifier = modifier
                ) {
                    filterTeamCategory = teamCategoryList[it]
                }
                ReadonlyViewText("Filter Association", modifier)
                DropdownList(
                    associationList.toImmutableList(),
                    associationList.indexOf(filterAssociation),
                    modifier = modifier
                ) {
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
                getFixtures(
                    state.values(),
                    competitionFilter,
                    filterAssociation,
                    filterTeamCategory,
                    teamCategoryState.values(),
                    teamCounts
                ) { date, teamCategory, message, home, away ->
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

// Export the fixtures for each association as a separate file with
// dates on rows and team categories on columns
// Forces boys to play on Saturday and girls either Friday or Sunday...
private fun crossTeamCategoryAssociationExport(saturdayFixtures: Boolean,
                                               folder: PlatformFile?,
                                               sourceFixtureValues: SourceFixtureValues,
                                               associations: List<Association>,
                                               competitionId: CompetitionId) {
    val type: String
    val teamCategoriesToUse: List<TeamCategory>
    val header: List<String>
    val prefix: (Int, Int) -> List<Any>

    if (saturdayFixtures) {
        type = "Boys"
        teamCategoriesToUse = sourceFixtureValues.teamCategories.filter { it.matchDay == Day.SAT.ordinal.toShort() }
        header = listOf("Date", "HomeCount") + teamCategoriesToUse.map {it.name}
        prefix = {week, homeCount -> listOf(DayDate(week).addDays(Day.SAT.ordinal).toString(), homeCount)}
    } else {
        type = "Girls"
        teamCategoriesToUse = sourceFixtureValues.teamCategories.filter { it.matchDay != Day.SAT.ordinal.toShort() }
        header = listOf("Date", "Alt Date", "HomeCount") + teamCategoriesToUse.map {it.name}
        prefix = {week, homeCount -> listOf(DayDate(week).addDays(Day.FRI.ordinal).toString(), DayDate(week).addDays(Day.SUN.ordinal).toString(), homeCount)}
    }
    val filterTeamCategories = teamCategoriesToUse.map { it.id }.toSet()

    associations.forEach { association ->
        val fixturesByDate = sourceFixtureValues.allFixtures
            .filter { it.competitionId == competitionId }
            .filter { it.homeAssociation.isBlank() || it.homeAssociation == association.name || it.awayAssociation == association.name }
            .filter { filterTeamCategories.contains(it.teamCategoryId) }
            .groupBy { it.date }
            .toSortedMap()
        val file = PlatformFile(
            Path(folder?.toKotlinxIoPath().toString()).resolve("${association.name}$type.csv").toFile()
        )
        val sink = file.sink(append = false).buffered()
        sink.use { bufferedSink ->
            val sb = StringBuilder()
            var df = DataFrame.emptyOf<Any?>()
            fixturesByDate.forEach { (date, views) ->
                val gameMap = views.groupBy { it.teamCategoryId }
                val homeGameCount = gameMap.count { it.value.isNotEmpty() && it.value[0].homeAssociation == association.name && it.value[0].homeTeamNumber > 0 && it.value[0].awayTeamNumber != 0.toShort() }
                val values = prefix(date, homeGameCount) + teamCategoriesToUse
                    .map { gameMap[it.id] }
                    .map {fixture -> gameDisplay(fixture, sourceFixtureValues.teamCounts) }
                df = df.concat(dataFrameOf(header)(*values.toTypedArray()))
            }
            df.writeCsv(sb)
            bufferedSink.writeString(sb.toString())
        }
    }
}

private fun gameDisplay(fixture: List<SeasonFixtureView>?, teamCounts: TeamCountMap): String =
    when {
        fixture.isNullOrEmpty() -> ""
        fixture.size == 1 -> "${fixture[0].message} ${teamName(fixture[0], true, teamCounts)} vs ${teamName(fixture[0], false, teamCounts)}"
        else -> {
            for (game in fixture) {
                println("${game.teamCategoryName} ${game.message} ${teamName(game, true, teamCounts)} vs ${teamName(game, false, teamCounts)}")
            }
            throw IllegalArgumentException("Multiple games ${fixture[0].teamCategoryName}")
        }
    }

private fun teamCategoryExportForLeagueRepublic(saturdayFixtures: Boolean,                               folder: PlatformFile?,
                                                fileName: String,
                                                sourceFixtureValues: SourceFixtureValues,
                                                fixtureFilter: FixtureFilter) {
    val file = PlatformFile(
        Path(folder?.toKotlinxIoPath().toString()).resolve("$fileName-LR.csv").toFile()
    )
    val sink = file.sink(append = false).buffered()
    sink.use { bufferedSink ->
        val dates: (Int) -> String = if (saturdayFixtures) {
            {week -> DayDate(week).addDays(Day.SAT.ordinal).toString()}
        } else {
            {week -> DayDate(week).addDays(Day.FRI.ordinal).toString()}
        }
        val sb = StringBuilder()
        var df = DataFrame.emptyOf<Any?>()
        sourceFixtureValues.allFixtures
            .filter { it.competitionId == fixtureFilter.filterCompetition }
            .filter { it.teamCategoryName == fixtureFilter.filterTeamCategory }
            .filter { it.homeAssociation.isNotBlank() && it.awayAssociation.isNotBlank() }
            .forEach {
                df = df.concat(dataFrameOf(
                    "Date" to listOf(dates(it.date)),
                    "Time" to listOf("00:00"),
                    "Division" to listOf(""),
                    "Home" to listOf(teamName(it, true, sourceFixtureValues.teamCounts)),
                    "Away" to listOf(teamName(it, false, sourceFixtureValues.teamCounts)),
                    "V" to listOf(""),
                    "P" to listOf(""),
                    "HS" to listOf(""),
                    "AS" to listOf("")
                ))
            }
        df.writeCsv(sb)
        bufferedSink.writeString(sb.toString())
    }
}

private fun spondExport(saturdayFixtures: Boolean,
                        folder: PlatformFile?,
                        fileName: String,
                        sourceFixtureValues: SourceFixtureValues,
                        fixtureFilter: FixtureFilter) {
    val adjustDate: (Int) -> String
    val startTime: () -> String
    when (saturdayFixtures) {
        true -> {
            adjustDate = {week -> DayDate(week).addDays(Day.SAT.ordinal).toString()}
            startTime = {"10:30:00"}
        }
        else -> {
            adjustDate = {week -> DayDate(week).addDays(Day.FRI.ordinal).toString()}
            startTime = {"18:30:00"}
        }
    }
    childCsvFile(folder, fileName).sink(append = false).buffered().use { bufferedSink ->
        val sb = StringBuilder()
        sourceFixtureValues.allFixtures
            .asSequence()
            .filter { it.competitionId == fixtureFilter.filterCompetition }
            .filter { it.teamCategoryName == fixtureFilter.filterTeamCategory }
            .filter { it.homeAssociation.isNotBlank() && it.awayAssociation.isNotBlank() }
            .filter { it.homeAssociation == fixtureFilter.filterAssociation || it.awayAssociation == fixtureFilter.filterAssociation }
            .fold(DataFrame.emptyOf<Any?>()) {df, it ->
                df.concat(dataFrameOf(
                    "Date" to listOf(adjustDate(it.date)),
                    "Time" to listOf(startTime()),
                    "Home" to listOf(teamName(it, true, sourceFixtureValues.teamCounts)),
                    "Away" to listOf(teamName(it, false, sourceFixtureValues.teamCounts)),
                ))
            }.writeCsv(sb)
        bufferedSink.writeString(sb.toString())
    }
}

private fun teamCategoryExport(saturdayFixtures: Boolean,
                               folder: PlatformFile?,
                               fileName: String,
                               sourceFixtureValues: SourceFixtureValues,
                               fixtureFilter: FixtureFilter) {
    val file = childCsvFile(folder, fileName)
    val sink = file.sink(append = false).buffered()
    sink.use { bufferedSink ->
        val dates: (Int) -> String = if (saturdayFixtures) {
            {week -> DayDate(week).addDays(Day.SAT.ordinal).toString()}
        } else {
            {week -> DayDate(week).addDays(Day.FRI.ordinal).toString()}
        }
        val sb = StringBuilder()
        var df = DataFrame.emptyOf<Any?>()
        sourceFixtureValues.allFixtures
            .filter { it.competitionId == fixtureFilter.filterCompetition }
            .filter { it.teamCategoryName == fixtureFilter.filterTeamCategory }
            .groupBy { it.date }
            .toSortedMap().forEach { (_, fixtures) ->
                val dateValues = listOf("") + fixtures.map { dates(it.date) }
                val messageValues = listOf("") + fixtures.map { it.message }
                val homeValues = listOf("") + fixtures.map { teamName(it, true, sourceFixtureValues.teamCounts) }
                val awayValues = listOf("") + fixtures.map { teamName(it, false, sourceFixtureValues.teamCounts) }
                df = if (saturdayFixtures) {
                    df.concat(dataFrameOf(
                        "Date" to dateValues,
                        "Message" to messageValues,
                        "Home" to homeValues,
                        "Away" to awayValues))
                } else {
                    val altDateValues = listOf("") + fixtures.map { DayDate(it.date).addDays(Day.SUN.ordinal).toString() }
                    df.concat(dataFrameOf(
                        "Date" to dateValues,
                        "Alt Date" to altDateValues,
                        "Message" to messageValues,
                        "Home" to homeValues,
                        "Away" to awayValues
                    ))
                }
            }
        df.writeCsv(sb)
        bufferedSink.writeString(sb.toString())
    }
}

private fun childCsvFile(folder: PlatformFile?, fileName: String): PlatformFile =
    PlatformFile(Path(folder?.toKotlinxIoPath().toString()).resolve("$fileName.csv").toFile())

private fun export(sourceFixtureValues: SourceFixtureValues,
                   fixtureFilter: FixtureFilter,
                   withTeamCategory: Boolean,
                   writer: Appendable,
                   addBlank: Boolean = false
) {
    var df = DataFrame.emptyOf<Any?>()
    var previousDate = ""
    getFixtures(sourceFixtureValues, fixtureFilter) { date, teamCategory, message, home, away ->
        df = if (withTeamCategory) {
            df.concat(
                dataFrameOf("Date", "Team Category", "Message", "Home", "Away")
                    (date, teamCategory, message, home, away)
            )
        } else {
            df.concat(if (addBlank && previousDate != date) {
                previousDate = date
                dataFrameOf(
                    "Date" to listOf("", date),
                    "Message" to listOf("", message),
                    "Home" to listOf("", home),
                    "Away" to listOf("", away))
            } else {
                dataFrameOf(
                    "Date" to listOf(date),
                    "Message" to listOf(message),
                    "Home" to listOf(home),
                    "Away" to listOf(away))
            })
        }
    }
    df.writeCsv(writer)
}

@Composable
fun FixtureDateScreen(season: Season) {
    val viewModel: SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val associationViewModel: AssociationViewModel = koinViewModel()
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()
    val seasonLeagueTeamModel: SeasonLeagueTeamViewModel = koinViewModel { parametersOf(season.id) }

    val associationState by associationViewModel.getState().collectAsStateWithLifecycle()
    val teamCategoryState by teamCategoryViewModel.getState().collectAsStateWithLifecycle()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val seasonLeagueTeamState by seasonLeagueTeamModel.getState().collectAsStateWithLifecycle()

    FixtureDateContent(
        season = season,
        associationState = associationState,
        teamCategoryState = teamCategoryState,
        state = state,
        seasonLeagueTeamState = seasonLeagueTeamState
    )
}

@Composable
private fun FixtureDateContent(
    season: Season,
    associationState: ViewModelState<Association>,
    teamCategoryState: ViewModelState<TeamCategory>,
    state: ViewModelState<SeasonFixtureView>,
    seasonLeagueTeamState: ViewModelState<SeasonLeagueTeamView>
) {
    var competitionFilter by remember { mutableStateOf(0.toShort()) }

    ViewCommon(
        "Season fixtures",
        "Return to seasons screen",
        states = persistentListOf(associationState, teamCategoryState)
    ) { paddingValues ->
        val columns = associationState.values().size + 2
        val teamCounts = seasonLeagueTeamState.values()
            .associateBy({ Triple(it.teamCategoryId, it.associationName, it.competitionId) }, { it.count })
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
                        item {
                            ReadonlyViewText(dateTotal[date].toString())
                        }
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

private fun adjustedWeek(fixture: SeasonFixtureView, matchDayAdjust: Map<String, Short>): Int =
    if (matchDayAdjust[fixture.teamCategoryName]!! > 0.toShort()) {
        DayDate(fixture.date).addDays(matchDayAdjust[fixture.teamCategoryName]!!.toInt()).value()
    } else {
        DayDate(fixture.date).value()
    }

private fun adjustedDate(fixture: SeasonFixtureView, matchDayAdjust: Map<String, Short>): String =
    DayDate(adjustedWeek(fixture, matchDayAdjust)).toString()

private data class SourceFixtureValues(val allFixtures: List<SeasonFixtureView>,
                                       val teamCategories: List<TeamCategory>,
                                       val teamCounts: TeamCountMap)

private data class FixtureFilter(val filterCompetition: CompetitionId, val filterAssociation: String, val filterTeamCategory: String)

private fun getFixtures(values: SourceFixtureValues, filter: FixtureFilter, fixtureConsumer: (String, String, String, String, String) -> Unit) {
    getFixtures(values.allFixtures, filter.filterCompetition, filter.filterAssociation, filter.filterTeamCategory, values.teamCategories, values.teamCounts, fixtureConsumer)
}

internal fun getFixtures(
    allFixtures: List<SeasonFixtureView>,
    filterCompetition: CompetitionId,
    filterAssociation: String,
    filterTeamCategory: String,
    teamCategories: List<TeamCategory>,
    teamCounts: TeamCountMap,
    fixtureConsumer: (String, String, String, String, String) -> Unit
) {
    val matchDayAdjust = teamCategories.associateBy({ it.name }, { it.matchDay })
    allFixtures.sortedBy { it.date }
        .filter { it.competitionId == filterCompetition }
        .filter {
            when {
                it.homeAssociation.isBlank() && (filterTeamCategory.isBlank() || it.teamCategoryName == filterTeamCategory) -> true
                filterAssociation.isNotBlank() && it.homeAssociation != filterAssociation && it.awayAssociation != filterAssociation -> false
                filterTeamCategory.isNotBlank() && it.teamCategoryName != filterTeamCategory -> false
                else -> true
            }
        }
        .sortedBy { adjustedWeek(it, matchDayAdjust) }
        .forEach {
            fixtureConsumer(
                adjustedDate(it, matchDayAdjust),
                it.teamCategoryName,
                it.message,
                teamName(it, true, teamCounts),
                teamName(it, false, teamCounts)
            )
        }
}

internal fun teamName(fixture: SeasonFixtureView, home: Boolean, teamCountMap: TeamCountMap?): String {
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

internal fun teamName(association: String, number: Short): String {
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
        .groupBy({ it.awayAssociation }, { it.homeAssociation })
        .mapValues { (_, values) -> values.toSet() }
    val currentHomeFixtureCount = mutableMapOf<Pair<Int, AssociationId>, Int>()
    val matchDayAdjust = teamCategoryDao.get().associateBy({ it.id }, { it.matchDay })

    for (activeLeagueCompetition in activeLeagueCompetitions) {
        for (activeTeamCategory in seasonTeamCategoryDao.getActiveTeamCategories(
            seasonId,
            activeLeagueCompetition.competitionId
        )) {
            seasonFixtureDao.deleteBySeasonTeamCategory(
                seasonId,
                activeTeamCategory.teamCategoryId,
                activeLeagueCompetition.competitionId
            )

            if (activeTeamCategory.games == 0.toShort()) {
                continue
            }

            for (seasonBreak in resolvedSeasonWeeks.breakWeeks()) {
                seasonFixtureDao.insert(
                    SeasonFixture(
                        0,
                        seasonId,
                        activeLeagueCompetition.competitionId,
                        activeTeamCategory.teamCategoryId,
                        seasonBreak.key,
                        seasonBreak.value,
                        0.toShort(),
                        0.toShort(),
                        0.toShort(),
                        0.toShort()
                    )
                )
            }
            leagueGames.prepareGames(
                activeLeagueCompetition.competitionId,
                activeTeamCategory.teamCategoryId,
                activeTeamCategory.games,
                seasonTeamDao.getTeams(
                    seasonId,
                    activeLeagueCompetition.competitionId,
                    activeTeamCategory.teamCategoryId
                ),
                farAwayGames
            )
        }
        seasonFixtureDao.get(seasonId, activeLeagueCompetition.competitionId)
            .groupBy {
                Pair(
                    DayDate(it.date).addDays(matchDayAdjust[it.teamCategoryId]?.toInt() ?: 0).value(),
                    it.homeAssociationId
                )
            }
            .mapValues { (id, list) -> currentHomeFixtureCount.merge(id, list.size, Int::plus) }
    }

    val teamsByCategoryAndCompetition = mutableMapOf<Pair<TeamCategoryId, CompetitionId>, Int>()
    for (seasonTeam in seasonTeamDao.getBySeason(seasonId)) {
        val key = Pair(seasonTeam.teamCategoryId, seasonTeam.competitionId)
        teamsByCategoryAndCompetition.merge(key, 0, Int::plus)
        teamsByCategoryAndCompetition[key] = teamsByCategoryAndCompetition.getOrPut(key) { 0 } + seasonTeam.count
    }

    for (fixture in leagueGames.scheduleFixtures(
        seasonId,
        resolvedSeasonWeeks,
        teamCategoryDao.get(),
        seasonTeamCategoryDao.getBySeasonId(seasonId),
        seasonCompRoundViewDao.get(seasonId),
        teamsByCategoryAndCompetition,
        activeLeagueCompetitions.map { it.competitionId }.toSet(),
        currentHomeFixtureCount
    )) {
        seasonFixtureDao.insert(fixture)
    }
}
