package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureView
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonCupSummaryView
import io.github.iandbrown.sportplanner.database.SeasonCupSummaryViewDao
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonRoundDao
import io.github.iandbrown.sportplanner.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.measureTime

private val editor = Editors.SEASON_CUP_FIXTURES

@Composable
internal fun NavigateCupFixtures(argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("View") -> CupFixtureView()
        argument.startsWith("Summary") -> {
            val param = argument.substring("Summary&".length)
            SummaryCupFixtureView(Json.decodeFromString<Season>(param))
        }
        else -> CupFixtureTableView(Json.decodeFromString<Season>(argument))
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun CupFixtureView(viewModel: SeasonViewModel= koinInject<SeasonViewModel>()) {
    val seasonState = viewModel.uiState.collectAsState()
    val calculating = remember {mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()

    if (calculating.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon("Cup Fixtures",
            states = persistentListOf(seasonState.value)) { paddingValues ->
            val surfaceColor = MaterialTheme.colorScheme.onSurface
            LazyVerticalGrid(columns = WeightedIconGridCells(3, 1), Modifier.padding(paddingValues)) {
                item { ViewText("Season") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (season in seasonState.values().sortedByDescending { it.name.trim().uppercase() }) {
                    item { ViewText(season.name) }
                    clickableIcon(Icons.Filled.Summarize, "Show fixture summaries", surfaceColor) {
                        "${editor.name}/Summary&${Json.encodeToString(season)}"
                    }
                    clickableIcon(Icons.Filled.GridView, "Show fixtures", surfaceColor) {
                        editor.editRoute(season)
                    }
                    item { ClickableIconOld(Icons.Filled.Calculate, "Calculate fixtures") {
                        calculating.value = true
                        coroutineScope.launch {
                            val timeTaken = measureTime { calcSeasonCupFixtures(season.id) }
                            println("Season Fixtures calculated in $timeTaken")
                        }
                        calculating.value = false
                    }}
                }
            }
        }
    }
}

internal class SeasonCupSummaryViewModel(seasonId : SeasonId,
                                         dao: SeasonCupSummaryViewDao = inject<SeasonCupSummaryViewDao>().value) :
    BaseSeasonReadViewModel<SeasonCupSummaryViewDao, SeasonCupSummaryView>(seasonId, dao)

internal class SeasonRoundViewModel(seasonId : SeasonId,
                                    dao: SeasonRoundDao = inject<SeasonRoundDao>().value) :
    BaseSeasonReadViewModel<SeasonRoundDao, SeasonCompetitionRound>(seasonId, dao)

// Show summary state for all cup competitions in the season
// - number of teams in the competition by team category
// - 'round state' - are the correct number of rounds defined
@Suppress("ParamsComparedByRef")
@Composable
private fun SummaryCupFixtureView(season: Season,
                                  seasonCupSummaryViewModel: SeasonCupSummaryViewModel = koinViewModel {parametersOf(season.id)},
                                  seasonRoundViewModel: SeasonRoundViewModel = koinViewModel {parametersOf(season.id)},
                                  competitionViewModel: CompetitionViewModel = koinViewModel()) {
    val seasonTeamState = seasonCupSummaryViewModel.uiState.collectAsState()
    val seasonRoundState = seasonRoundViewModel.uiState.collectAsState()
    val competitionState = competitionViewModel.uiState.collectAsState()

    ViewCommon("Season Cup summary ${season.name}",
        states = persistentListOf(seasonTeamState.value, seasonRoundState.value, competitionState.value)) { paddingValues ->
        val data = seasonTeamState.values()
            .groupBy { it.competitionName }
            .mapValues { (_, list) -> list.groupBy { it.teamCategoryName }}
        val competitionNameLookup = competitionState.values().associateBy ({ it.id }, {it.name} )
        val roundsByCompetition = seasonRoundState.values().groupBy { competitionNameLookup[it.competitionId] }
        LazyVerticalGrid(columns = WeightedIconGridCells(0, 2, 1, 2, 1, 2), Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Competition", "Rounds", "Team Category", "Team Count", "State"))
            for ((competitionName, v) in data) {
                var first = true
                for ((teamCategoryName, v2) in v) {
                    val roundCount = roundsByCompetition[v2[0].competitionName]?.size ?: 0
                    val teamCount = v2.sumOf { it.count.toInt() }
                    if (first) {
                        viewTextItems(listOf(competitionName, roundCount.toString(), "", "", roundsState(roundsByCompetition[competitionName] ?: emptyList())))
                        first = false
                    }
                    viewTextItems(listOf("", "", teamCategoryName,  teamCount.toString(), competitionState(teamCount, roundCount)))
                }
            }
        }
    }
}

private fun roundsState(rounds: List<SeasonCompetitionRound>) : String {
    var week = -1

    for (round in rounds) {
        if (round.week <= week) {
            return "ROUNDS OUT OF ORDER"
        }
        week = round.week
    }
    return ""
}

private fun competitionState(teamCount: Int, roundCount: Int) : String {
    var gameCount = calculateGameCount(teamCount)
    var requiredRounds = 0
    while (gameCount > 0) {
        ++requiredRounds
        gameCount /= 2
    }
    return when  {
        roundCount == requiredRounds -> ""
        roundCount < requiredRounds -> "TOO FEW ROUNDS"
        else -> "TOO MANY ROUNDS"
    }
}

internal class SeasonCupFixtureViewModel(seasonId : SeasonId,
                                         dao: SeasonCupFixtureViewDao = inject<SeasonCupFixtureViewDao>().value) :
    BaseSeasonReadViewModel<SeasonCupFixtureViewDao, SeasonCupFixtureView>(seasonId, dao) {
    fun setResult(id: Long, result: Short) = viewModelScope.launch {  dao.setResult(id, result) }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun CupFixtureTableView(season: Season,
                                viewModel: SeasonCupFixtureViewModel = koinInject<SeasonCupFixtureViewModel> { parametersOf(season.id) },
                                competitionViewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()) {
    val state = viewModel.uiState.collectAsState()
    val competitionState = competitionViewModel.uiState.collectAsState()
    val edits = remember { mutableStateMapOf<Long, Short>() }
    var isLocked by remember { mutableStateOf(true) }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""

    ViewCommon("Cup Fixtures ${season.name}",
        states = persistentListOf(state.value, competitionState.value),
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(buttonText) {
                    if (!isLocked && edits.isNotEmpty()) {
                        saveResults(edits, viewModel)
                    }
                    isLocked = !isLocked
                }
            )
        },
        confirm = { edits.isNotEmpty() },
        confirmAction = { saveResults(edits, viewModel) }) { paddingValues ->
        val competitionNameLookup = competitionState.values().associateBy ({ it.id }, {it.name} )
        LazyVerticalGrid(columns = WeightedIconGridCells(0, 1, 1, 1, 1, 1, 1), Modifier.padding(paddingValues)) {
            val fixturesById = state.values().associateBy { it.id }
            viewTextItems(listOf("Competition", "Team Category", "Round", "Home", "Away", "Winner"))
            var competitionId: CompetitionId = 0
            for (fixture in state.values().sortedWith(compareBy({competitionNameLookup[it.competitionId] ?: ""}, {it.teamCategoryName}))) {
                if (fixture.competitionId != competitionId) {
                    viewTextItems(listOf(competitionNameLookup[fixture.competitionId] ?: "", "", "", "", "", ""))
                    competitionId = fixture.competitionId
                }
                viewTextItems(
                    listOf(
                        "",
                        fixture.teamCategoryName,
                        fixture.round.toString(),
                        teamDescription(fixturesById, fixture.homePending, fixture.homeAssociation, fixture.homeTeamNumber),
                        teamDescription(fixturesById, fixture.awayPending, fixture.awayAssociation, fixture.awayTeamNumber),
                    )
                )
                item {
                    DropdownList(
                        itemList = FixtureResult.entries.map { it.display }.toImmutableList(),
                        selectedIndex = edits[fixture.id]?.toInt() ?: fixture.result.toInt(),
                        isLocked = { isLocked || fixture.awayAssociation.isBlank() },
                    ) {
                        if (it == 0) {
                            edits.remove(fixture.id)
                        } else {
                            edits[fixture.id] = it.toShort()
                        }
                    }
                }
            }
        }
    }
}

private fun saveResults(edits: MutableMap<Long, Short>, viewModel: SeasonCupFixtureViewModel) {
    edits.forEach { (key, result) ->
        viewModel.setResult(key, result)
    }
    edits.clear()
}

internal suspend fun calcSeasonCupFixtures(seasonId: SeasonId,
                                           dao : SeasonCompetitionRoundDao = inject<SeasonCompetitionRoundDao>().value) {
    println("Calculating fixtures for season $seasonId")
    dao.getUnstartedRounds(seasonId).forEach {
        println("Calculating fixtures for ${it.competitionId} round ${it.round}")
        calcCupFixtures(seasonId, it.competitionId, it.round)
    }
}
