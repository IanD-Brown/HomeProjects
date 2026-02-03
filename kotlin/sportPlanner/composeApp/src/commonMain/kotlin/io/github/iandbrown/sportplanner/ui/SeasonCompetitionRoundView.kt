package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.AssociationName
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.database.SeasonCupFixture
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureView
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.TeamNumber
import io.github.iandbrown.sportplanner.logic.DayDate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import kotlin.random.Random
import kotlin.time.measureTime
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class SeasonCompetitionRoundViewModel(seasonId: SeasonId, competitionId: CompetitionId) :
    BaseSeasonCompCRUDViewModel<SeasonCompetitionRoundDao, SeasonCompetitionRound>(
        seasonId,
        competitionId,
        inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value
    )

class SeasonCupFixtureViewModel(seasonId: SeasonId, competitionId: CompetitionId) :
    BaseSeasonCompReadViewModel<SeasonCupFixtureViewDao, SeasonCupFixtureView>(
        seasonId,
        competitionId,
        inject<SeasonCupFixtureViewDao>(SeasonCupFixtureViewDao::class.java).value
    ) {
    fun setResult(id: Long, result: Short) = viewModelScope.launch {  dao.setResult(id, result) }
}

class SeasonCompetitionViewModel(seasonId: SeasonId, competitionId: CompetitionId) :
    BaseSeasonCompCRUDViewModel<SeasonCompetitionDao, SeasonCompetition>(
        seasonId,
        competitionId,
        inject<SeasonCompetitionDao>(SeasonCompetitionDao::class.java).value
    )


private val editor: Editors = Editors.SEASON_COMPETITION_ROUND

@Serializable
private data class SeasonCompetitionRoundEditorInfo(
    val param: SeasonCompetitionParam,
    val competitionRound: SeasonCompetitionRound? = null
)

@Composable
fun NavigateSeasonCompetitionRound(argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonCompetitionView(
            Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5))
        )
        argument.startsWith("Fixtures&") -> SeasonCupFixtureView(
            Json.decodeFromString<SeasonCompetitionRoundEditorInfo>(argument.substring(9))
        )
        else -> SeasonCompetitionRoundEditor(
            Json.decodeFromString<SeasonCompetitionRoundEditorInfo>(argument)
        )
    }
}

@Composable
private fun SeasonCompetitionView(param: SeasonCompetitionParam) {
    val viewModel: SeasonCompetitionRoundViewModel = koinInject { parametersOf(param.seasonId, param.competitionId) }
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    var calculating by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val seasonCompetitionViewModel = koinInject<SeasonCompetitionViewModel> { parametersOf(param.seasonId, param.competitionId) }
    val seasonCompetitionState = seasonCompetitionViewModel.uiState.collectAsState()

    if (calculating) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon(
            "Competition rounds in ${param.seasonName} for ${param.competitionName}",
            bottomBar = {
                BottomBarWithButtonN("+", seasonCompetitionState.value.isNotEmpty()) {
                    editor.editRoute(SeasonCompetitionRoundEditorInfo(param))
                }
            },
            content = { paddingValues ->
                val values = state.value.filter { it.competitionId == param.competitionId }
                    .sortedBy { it.round }
                LazyVerticalGrid(
                    columns = WeightedIconGridCells(4, 1, 2, 2, 2),
                    Modifier.padding(paddingValues),
                    gridState
                ) {
                    item { ViewText("Round") }
                    item { ViewText("Description") }
                    item { ViewText("Week") }
                    item { ViewText("Optional") }
                    item { Icon(Blank, "") }
                    item { Icon(Blank, "") }
                    item { Icon(Blank, "") }
                    item { Icon(Blank, "") }
                    for (it in values) {
                        item { ViewText(it.round.toString()) }
                        item { ViewText(it.description) }
                        item { ViewText(DayDate(it.week).toString()) }
                        item { Checkbox(checked = it.optional, onCheckedChange = null, enabled = false) }
                        item {
                            ClickableIcon(Icons.Filled.GridView, "Show fixtures") {
                                "${editor.name}/Fixtures&${
                                    Json.encodeToString(
                                        SeasonCompetitionRoundEditorInfo(param, it)
                                    )
                                }"
                            }
                        }
                        item {
                            ClickableIconOld(Icons.Filled.Calculate, "Calculate fixtures") {
                                calculating = true
                                coroutineScope.launch {
                                    val timeTaken = measureTime {
                                        calcCupFixtures(param.seasonId, param.competitionId, it.round)
                                    }
                                    println("Cup fixtures calculated in $timeTaken")
                                }
                                calculating = false
                            }
                        }
                        item {
                            EditButton {
                                editor.editRoute(SeasonCompetitionRoundEditorInfo(param, it))
                            }
                        }
                        item {
                            DeleteButton(it != values[values.lastIndex]) {
                                viewModel.delete(it)
                            }
                        }
                    }
                }
            })
    }
}

@Composable
private fun SeasonCompetitionRoundEditor(info: SeasonCompetitionRoundEditorInfo) {
    val viewModel: SeasonCompetitionRoundViewModel = koinInject { parametersOf(info.param.seasonId, info.param.competitionId) }
    val state = viewModel.uiState.collectAsState()
    val seasonCompetitionViewModel = koinInject<SeasonCompetitionViewModel> { parametersOf(info.param.seasonId, info.param.competitionId) }
    val seasonCompetitionState = seasonCompetitionViewModel.uiState.collectAsState()
    val description = remember { mutableStateOf("") }
    val week = remember { mutableIntStateOf(0) }
    val optional = remember { mutableStateOf(false) }
    val validRound = remember { mutableStateOf(true) }
    val title =
        if (info.competitionRound == null) "Add Competition round" else "Edit Competition round"
    val round = remember { mutableStateOf<Short>(0) }

    ViewCommon(
        title,
        bottomBar = {
            BottomBarWithButton(enabled = !description.value.isEmpty() && validRound.value) {
                when (info.competitionRound) {
                    is SeasonCompetitionRound -> {
                        viewModel.update(
                            SeasonCompetitionRound(
                                info.competitionRound.seasonId,
                                info.competitionRound.competitionId,
                                round.value,
                                description.value.trim(),
                                week.intValue,
                                optional.value
                            )
                        )
                    }
                    else -> {
                        viewModel.insert(
                            SeasonCompetitionRound(
                                info.param.seasonId,
                                info.param.competitionId,
                                round.value,
                                description.value.trim(),
                                week.intValue,
                                optional.value
                            )
                        )
                    }
                }
                appNavController.popBackStack()
            }
        }) { paddingValues ->
        val rounds = getRounds(state.value)

        when (info.competitionRound) {
            is SeasonCompetitionRound -> {
                round.value = info.competitionRound.round
                description.value = info.competitionRound.description
                week.intValue = info.competitionRound.week
                optional.value = info.competitionRound.optional
            }

            else -> {
                if (rounds.isEmpty()) {
                    round.value = 1.toShort()
                } else {
                    round.value = (rounds.max() + 1).toShort()
                }
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(4), Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Round") }
            item { ReadonlyViewText("Description") }
            item { ReadonlyViewText("Week") }
            item { ReadonlyViewText("Optional") }
            item {
                ViewTextField(
                    value = round.value.toString(),
                    modifier = Modifier,
                    isError = { !validRound.value }) {
                    try {
                        round.value = it.toShort()
                        validRound.value = round.value > 0 &&
                                (info.competitionRound != null && info.competitionRound.round == round.value
                                        || !rounds.contains(round.value))
                    } catch (_: NumberFormatException) {
                        validRound.value = false
                    }
                }
            }
            item { ViewTextField(description.value) { description.value = it } }
            item {
                DatePickerView(week.intValue,
                    Modifier,
                    { utcMs -> isMondayIn(seasonCompetitionState.value.first(), utcMs) }) {
                    week.intValue = it
                }
            }
            item { Checkbox(checked = optional.value, onCheckedChange = { optional.value = it }) }
        }
    }
}

private enum class FixtureResult(var display: String) {
    UN_PLAYED(""), HOME_WIN("Home win"), AWAY_WIN(
        "Away win"
    )
}

@Composable
private fun SeasonCupFixtureView(info: SeasonCompetitionRoundEditorInfo) {
    val coroutineScope = rememberCoroutineScope()
    val seasonCompParameter = parametersOf(info.param.seasonId, info.param.competitionId)
    val viewModel = koinInject<SeasonCupFixtureViewModel> { seasonCompParameter }
    val state = viewModel.uiState.collectAsState(emptyList())
    val teamCategoryState by koinInject<TeamCategoryViewModel>().uiState.collectAsState(emptyList())
    val edits = remember { mutableStateMapOf<Long, Short>() }
    var isLocked by remember { mutableStateOf(true) }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""
    val filterTeamCategory = remember { mutableStateOf("") }
    var fixturesById: Map<Long, SeasonCupFixtureView> = mutableMapOf()
    val withTeamCategory = filterTeamCategory.value.isBlank()

    ViewCommon(
        "${info.param.seasonName} ${info.param.competitionName} Round ${info.competitionRound?.round} Fixtures",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Export", edits.isEmpty()) {
                    coroutineScope.launch {
                        val file =
                            FileKit.openFileSaver(suggestedName = "cupFixtures", extension = "csv")
                        val sink = file?.sink(append = false)?.buffered()

                        sink.use { bufferedSink ->
                            if (withTeamCategory) {
                                bufferedSink?.writeString("Team Category,")
                            }
                            bufferedSink?.writeString("Home,Away\n")
                            getFixtures(state.value, info.competitionRound?.round!!, filterTeamCategory.value, fixturesById) {
                                    teamCategoryName, home, away, _, _, _ ->
                                if (withTeamCategory) {
                                    bufferedSink?.writeString("$teamCategoryName,")
                                }
                                bufferedSink?.writeString("$home,$away\n")
                            }
                        }
                    }
                },
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
        fixturesById = state.value.associateBy { it.id }
        val teamCategoryList = listOf("") + teamCategoryState.map { it.name }
        val columns = if (withTeamCategory) 4 else 3

        LazyVerticalGrid(columns = GridCells.Fixed(columns), Modifier.padding(paddingValues)) {
            item(span = { GridItemSpan(columns) }) {
                DropdownList(
                    teamCategoryList,
                    0,
                    label = "Filter Team Category"
                ) { filterTeamCategory.value = teamCategoryList[it] }
            }
            if (withTeamCategory) {
                item { ViewText("Team Category") }
            }
            item { ViewText("Home") }
            item { ViewText("Away") }
            item { ViewText("Winner") }
            getFixtures(state.value, info.competitionRound?.round!!, filterTeamCategory.value, fixturesById) {
                teamCategoryName, home, away, result, fixtureId, blankAwayAssociation ->
                if (withTeamCategory) {
                    item { ViewText(teamCategoryName) }
                }
                item { ViewText(home) }
                item { ViewText(away) }
                item {
                    DropdownList(
                        itemList = FixtureResult.entries.map { it.display },
                        selectedIndex = result.toInt(),
                        isLocked = { isLocked || blankAwayAssociation },
                    ) { edits[fixtureId] = it.toShort() }
                }
            }
        }
    }
}

private fun getFixtures(
    allFixtures: List<SeasonCupFixtureView>,
    round: Short,
    filterTeamCategory: String,
    fixturesById: Map<Long, SeasonCupFixtureView>,
    fixtureConsumer: (String, String, String, Short, Long, Boolean) -> Unit
) {
    allFixtures
        .filter { it.round == round && (filterTeamCategory.isBlank() || filterTeamCategory == it.teamCategoryName)  }
        .forEach {fixture ->
            fixtureConsumer(
                fixture.teamCategoryName,
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
                fixture.result,
                fixture.id,
                fixture.awayAssociation.isBlank())
        }
}


private fun teamDescription(
    fixturesById: Map<Long, SeasonCupFixtureView>,
    pendingFixtureId: Long,
    associationName: AssociationName,
    teamNumber: TeamNumber
): String {
    if (pendingFixtureId > 0) {
        val previousFixture = fixturesById[pendingFixtureId]
        if (previousFixture != null) {
            val homeTeam = teamDescription(
                fixturesById,
                -1L,
                previousFixture.homeAssociation,
                previousFixture.homeTeamNumber
            )
            val awayTeam = teamDescription(
                fixturesById,
                -1L,
                previousFixture.awayAssociation,
                previousFixture.awayTeamNumber
            )
            return when (previousFixture.result) {
                FixtureResult.HOME_WIN.ordinal.toShort() -> homeTeam
                FixtureResult.AWAY_WIN.ordinal.toShort() -> awayTeam
                else -> if (awayTeam.isNotBlank()) "$homeTeam OR $awayTeam" else homeTeam
            }
        }
    }
    return teamName(associationName, teamNumber)
}

private fun saveResults(edits: SnapshotStateMap<Long, Short>, viewModel: SeasonCupFixtureViewModel) {
    edits.forEach { (key, result) ->
        viewModel.setResult(key, result)
    }
    edits.clear()
}

private fun getRounds(data: List<SeasonCompetitionRound>): Set<Short> =
    data.map { it.round }.toSet()

data class CupFixtureTeams(
    val homeAssociation: AssociationId,
    val homeTeamNumber: TeamNumber,
    val awayAssociation: AssociationId,
    val awayTeamNumber: TeamNumber
)

private suspend fun calcCupFixtures(
    seasonId: SeasonId,
    competitionId: CompetitionId,
    round: Short
) {
    val db: AppDatabase by inject(AppDatabase::class.java)
    val seasonTeamDao = db.getSeasonTeamDao()
    val dao = db.getSeasonCupFixtureDao()

    dao.deleteByRound(seasonId, competitionId, round)

    val teamCategories = db.getTeamCategoryDao().getAsList()
    for (teamCategory in teamCategories) {
        if (round == 1.toShort()) {
            val competitionTeams =
                seasonTeamDao.getTeams(seasonId, competitionId, teamCategory.id)
                    .flatMap { seasonTeam ->
                        (0..<seasonTeam.count).map { teamNumber ->
                            Pair(seasonTeam.associationId, teamNumber.toShort())
                        }
                    }.shuffled(Random(System.currentTimeMillis()))
            val games = roundUpToNextPowerOfTwo(competitionTeams.size) / 2

            planFixtures(games, competitionTeams)
                .forEach {
                    dao.insert(
                        SeasonCupFixture(
                            seasonId = seasonId,
                            competitionId = competitionId,
                            round = round,
                            teamCategoryId = teamCategory.id,
                            homeAssociationId = it.homeAssociation,
                            homeTeamNumber = it.homeTeamNumber,
                            awayAssociationId = it.awayAssociation,
                            awayTeamNumber = it.awayTeamNumber
                        )
                    )
                }
        } else {
            val previousRoundFixtures =
                dao.get(seasonId, competitionId, teamCategory.id, (round - 1).toShort())
                    .shuffled(Random(System.currentTimeMillis()))
            for (i in 0..<previousRoundFixtures.size step 2) {
                val home = previousRoundFixtures[i]
                val away = previousRoundFixtures[i + 1]
                dao.insert(
                    SeasonCupFixture(
                        seasonId = seasonId,
                        competitionId = competitionId,
                        round = round,
                        teamCategoryId = teamCategory.id,
                        homeAssociationId = winningAssociation(home),
                        homeTeamNumber = winningTeamNumber(home),
                        awayAssociationId = winningAssociation(away),
                        awayTeamNumber = winningTeamNumber(away),
                        homePending = if (home.result == 0.toShort()) home.id else 0L,
                        awayPending = if (away.result == 0.toShort()) away.id else 0L
                    )
                )
            }
        }
    }
}

private fun winningAssociation(fixture: SeasonCupFixture): AssociationId = when (fixture.result) {
    FixtureResult.HOME_WIN.ordinal.toShort() -> fixture.homeAssociationId
    FixtureResult.AWAY_WIN.ordinal.toShort() -> fixture.awayAssociationId
    else -> if (fixture.awayAssociationId == 0.toShort()) fixture.homeAssociationId else 0.toShort()
}

private fun winningTeamNumber(fixture: SeasonCupFixture): TeamNumber = when (fixture.result) {
    1.toShort() -> fixture.homeTeamNumber
    2.toShort() -> fixture.awayTeamNumber
    else -> if (fixture.awayAssociationId == 0.toShort()) fixture.homeTeamNumber else 0.toShort()
}

fun planFixtures(
    gameCount: Int,
    teams: List<Pair<AssociationId, TeamNumber>>
): List<CupFixtureTeams> {
    val result = mutableListOf<CupFixtureTeams>()
    val competitionTeams = teams.toMutableList()
    for (i in gameCount - 1 downTo 0) {
        val bye = competitionTeams.size / 2 <= i
        val team = competitionTeams.removeAt(0)
        val team2 = if (bye) {
            Pair(0.toShort(), 0.toShort())
        } else {
            competitionTeams.removeAt(0)
        }
        result.add(CupFixtureTeams(team.first, team.second, team2.first, team2.second))
    }
    return result
}

fun roundUpToNextPowerOfTwo(x: Int): Int {
    if (x <= 0) return 1
    var n = x - 1
    n = n or (n shr 1)
    n = n or (n shr 2)
    n = n or (n shr 4)
    n = n or (n shr 8)
    n = n or (n shr 16)
    return n + 1
}
