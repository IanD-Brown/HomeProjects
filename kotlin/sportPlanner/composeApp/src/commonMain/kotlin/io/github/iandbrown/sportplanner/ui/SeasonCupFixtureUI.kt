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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.time.measureTime

class SeasonCupSummaryViewModel(seasonId: SeasonId, dao: SeasonCupSummaryViewDao) :
    BaseReadViewModel<SeasonCupSummaryViewDao, SeasonCupSummaryView>(dao, { it.get(seasonId) })

class SeasonRoundViewModel(seasonId: SeasonId, dao: SeasonRoundDao) :
    BaseReadViewModel<SeasonRoundDao, SeasonCompetitionRound>(dao, { it.get(seasonId) })

class SeasonCupFixtureViewModel(seasonId: SeasonId, dao: SeasonCupFixtureViewDao) :
    BaseReadViewModel<SeasonCupFixtureViewDao, SeasonCupFixtureView>(dao, { it.get(seasonId) }) {

    fun saveResults(edits: Map<Long, Short>) {
        viewModelScope.launch {
            try {
                edits.forEach { (key, result) ->
                    dao.setResult(key, result)
                }
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}

@Composable
internal fun CupFixtureScreen() {
    val viewModel: SeasonViewModel = koinViewModel()
    val seasonState by viewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    var calculating by remember { mutableStateOf(false) }

    if (calculating) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon("Cup Fixtures",
            states = persistentListOf(seasonState)) { paddingValues ->
            val surfaceColor = MaterialTheme.colorScheme.onSurface
            LazyVerticalGrid(columns = WeightedIconGridCells(3, 1), Modifier.padding(paddingValues)) {
                item { ViewText("Season") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (season in seasonState.values().sortedByDescending { it.name.trim().uppercase() }) {
                    item { ViewText(season.name) }
                    clickableIcon(Icons.Filled.Summarize, "Show fixture summaries", surfaceColor) {
                        appNavigator.navigate(Route.CupFixturesSummary(season))
                    }
                    clickableIcon(Icons.Filled.GridView, "Show fixtures", surfaceColor) {
                        appNavigator.navigate(Route.CupFixturesTable(season))
                    }
                    item {
                        ClickableIconOld(Icons.Filled.Calculate, "Calculate fixtures") {
                            calculating = true
                            coroutineScope.launch {
                                val timeTaken = measureTime { calcSeasonCupFixtures(season.id) }
                                println("Season Fixtures calculated in $timeTaken")
                                calculating = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SummaryCupFixtureScreen(season: Season) {
    val seasonCupSummaryViewModel: SeasonCupSummaryViewModel = koinViewModel { parametersOf(season.id) }
    val seasonRoundViewModel: SeasonRoundViewModel = koinViewModel { parametersOf(season.id) }
    val competitionViewModel: CompetitionViewModel = koinViewModel()

    val seasonTeamState by seasonCupSummaryViewModel.getState().collectAsStateWithLifecycle()
    val seasonRoundState by seasonRoundViewModel.getState().collectAsStateWithLifecycle()
    val competitionState by competitionViewModel.getState().collectAsStateWithLifecycle()

    LifecycleResumeEffect(seasonCupSummaryViewModel) {
        seasonCupSummaryViewModel.readAll()
        seasonRoundViewModel.readAll()
        onPauseOrDispose { }
    }

    ViewCommon("Season Cup summary ${season.name}",
        states = persistentListOf(seasonTeamState, seasonRoundState, competitionState)) { paddingValues ->
        val data = seasonTeamState.values()
            .groupBy { it.competitionName }
            .mapValues { (_, list) -> list.groupBy { it.teamCategoryName } }
        val competitionNameLookup = competitionState.values().associateBy({ it.id }, { it.name })
        val roundsByCompetition = seasonRoundState.values().groupBy { competitionNameLookup[it.competitionId] }
        LazyVerticalGrid(columns = WeightedIconGridCells(0, 2, 1, 2, 1, 2), Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Competition", "Rounds", "Team Category", "Team Count", "State"))
            for ((competitionName, v) in data) {
                var first = true
                for ((teamCategoryName, v2) in v) {
                    val roundCount = roundsByCompetition[v2[0].competitionName]?.size ?: 0
                    val teamCount = v2.sumOf { it.count.toInt() }
                    if (first) {
                        viewTextItems(
                            listOf(
                                competitionName,
                                roundCount.toString(),
                                "",
                                "",
                                roundsState(roundsByCompetition[competitionName] ?: emptyList())
                            )
                        )
                        first = false
                    }
                    viewTextItems(
                        listOf(
                            "",
                            "",
                            teamCategoryName,
                            teamCount.toString(),
                            competitionStateText(teamCount, roundCount)
                        )
                    )
                }
            }
        }
    }
}

private fun roundsState(rounds: List<SeasonCompetitionRound>): String {
    var week = -1

    for (round in rounds) {
        if (round.week <= week) {
            return "ROUNDS OUT OF ORDER"
        }
        week = round.week
    }
    return ""
}

private fun competitionStateText(teamCount: Int, roundCount: Int): String {
    var gameCount = calculateGameCount(teamCount)
    var requiredRounds = 0
    while (gameCount > 0) {
        ++requiredRounds
        gameCount /= 2
    }
    return when {
        roundCount == requiredRounds -> ""
        roundCount < requiredRounds -> "TOO FEW ROUNDS"
        else -> "TOO MANY ROUNDS"
    }
}

@Composable
internal fun CupFixtureTableScreen(season: Season) {
    val viewModel: SeasonCupFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val competitionViewModel: CompetitionViewModel = koinViewModel()

    val state by viewModel.getState().collectAsStateWithLifecycle()
    val competitionState by competitionViewModel.getState().collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    val edits = remember { mutableStateMapOf<Long, Short>() }
    var isLocked by remember { mutableStateOf(true) }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""

    ViewCommon("Cup Fixtures ${season.name}",
        states = persistentListOf(state, competitionState),
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(buttonText) {
                    if (!isLocked && edits.isNotEmpty()) {
                        viewModel.saveResults(edits)
                        edits.clear()
                    }
                    isLocked = !isLocked
                }
            )
        },
        confirm = { edits.isNotEmpty() },
        confirmAction = {
            viewModel.saveResults(edits)
            edits.clear()
        }) { paddingValues ->
        val competitionNameLookup = competitionState.values().associateBy({ it.id }, { it.name })
        LazyVerticalGrid(columns = WeightedIconGridCells(0, 1, 1, 1, 1, 1, 1), Modifier.padding(paddingValues)) {
            val fixturesById = state.values().associateBy { it.id }
            viewTextItems(listOf("Competition", "Team Category", "Round", "Home", "Away", "Winner"))
            var competitionId: CompetitionId = 0
            for (fixture in state.values()
                .sortedWith(compareBy({ competitionNameLookup[it.competitionId] ?: "" }, { it.teamCategoryName }))) {
                if (fixture.competitionId != competitionId) {
                    viewTextItems(listOf(competitionNameLookup[fixture.competitionId] ?: "", "", "", "", "", ""))
                    competitionId = fixture.competitionId
                }
                viewTextItems(
                    listOf(
                        "",
                        fixture.teamCategoryName,
                        fixture.round.toString(),
                        teamDescription(
                            fixturesById,
                            fixture.homePending,
                            fixture.homeAssociation,
                            fixture.homeTeamNumber
                        ),
                        teamDescription(
                            fixturesById,
                            fixture.awayPending,
                            fixture.awayAssociation,
                            fixture.awayTeamNumber
                        ),
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

internal suspend fun calcSeasonCupFixtures(
    seasonId: SeasonId,
    dao: SeasonCompetitionRoundDao = inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value
) {
    println("Calculating fixtures for season $seasonId")
    dao.getUnstartedRounds(seasonId).forEach {
        println("Calculating fixtures for ${it.competitionId} round ${it.round}")
        calcCupFixtures(seasonId, it.competitionId, it.round)
    }
}
