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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.AssociationName
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.database.SeasonCupCompFixtureViewDao
import io.github.iandbrown.sportplanner.database.SeasonCupFixture
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureDao
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureView
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamNumber
import io.github.iandbrown.sportplanner.logic.DayDate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.random.Random
import kotlin.time.measureTime

class SeasonCompetitionRoundViewModel(
    val seasonId: SeasonId,
    val competitionId: CompetitionId,
    dao: SeasonCompetitionRoundDao
) : BaseCRUDViewModel<SeasonCompetitionRoundDao, SeasonCompetitionRound>(dao, { it.get(seasonId, competitionId) }) {

    fun save(competitionRound: SeasonCompetitionRound?, round: Short, description: String, week: Int, optional: Boolean) {
        if (competitionRound != null) {
            update(SeasonCompetitionRound(competitionRound.seasonId, competitionRound.competitionId, round, description.trim(), week, optional))
        } else {
            insert(SeasonCompetitionRound(seasonId, competitionId, round, description.trim(), week, optional))
        }
    }
}

class SeasonCompCupFixtureViewModel(seasonId: SeasonId, competitionId: CompetitionId, dao: SeasonCupCompFixtureViewDao
) : BaseReadViewModel<SeasonCupCompFixtureViewDao, SeasonCupFixtureView>(dao, { it.get(seasonId, competitionId) }) {
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

class SeasonCompetitionViewModel(seasonId: SeasonId, competitionId: CompetitionId, dao: SeasonCompetitionDao) :
    BaseCRUDViewModel<SeasonCompetitionDao, SeasonCompetition>(dao, { it.get(seasonId, competitionId) })

@Composable
fun SeasonCompetitionRoundListScreen(param: SeasonCompetitionParam) {
    val viewModel: SeasonCompetitionRoundViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var calculating by remember { mutableStateOf(false) }

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    if (calculating) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        SeasonCompetitionRoundListContent(
            param = param,
            state = state,
            onExport = {
                exportButtonSettings(coroutineScope, "cupRounds") {
                    toDataFrame(
                        state.values(),
                        inject<SeasonCompViewDao>(SeasonCompViewDao::class.java).value.getAsList(param.seasonId),
                        inject<CompetitionDao>(CompetitionDao::class.java).value.get()
                    ).writeJson(it)
                }
            },
            onImport = {
                ButtonSettings(imageVector = Icons.Default.Upload) {
                    viewModel.viewModelScope.launch {
                        tryTransaction({ viewModel.handleException(it) }, {
                            importFromFile(
                                "json",
                                {
                                    val dataFrame = DataFrame.readJson(it)
                                    viewModel.dao.deleteBySeasonComp(param.seasonId, param.competitionId)
                                    dataFrame
                                },
                                { viewModel.insert(toSeasonCompetitionRound(it, param.seasonId, param.competitionId)) })
                        })
                    }
                }
            },
            onAdd = { appNavigator.navigate(Route.SeasonCompetitionRoundEdit(param)) },
            onShowFixtures = { appNavigator.navigate(Route.SeasonCupFixtures(param, it)) },
            onCalculateFixtures = { round ->
                calculating = true
                coroutineScope.launch {
                    val timeTaken = measureTime {
                        calcCupFixtures(param.seasonId, param.competitionId, round)
                    }
                    println("Cup fixtures calculated in $timeTaken")
                    calculating = false
                }
            },
            onEdit = { appNavigator.navigate(Route.SeasonCompetitionRoundEdit(param, it)) },
            onDelete = { viewModel.delete(it) }
        )
    }
}

@Composable
private fun SeasonCompetitionRoundListContent(
    param: SeasonCompetitionParam,
    state: ViewModelState<SeasonCompetitionRound>,
    onExport: () -> ButtonSettings,
    onImport: () -> ButtonSettings,
    onAdd: () -> Unit,
    onShowFixtures: (SeasonCompetitionRound) -> Unit,
    onCalculateFixtures: (Short) -> Unit,
    onEdit: (SeasonCompetitionRound) -> Unit,
    onDelete: (SeasonCompetitionRound) -> Unit
) {
    val gridState = rememberLazyGridState()

    ViewCommon(
        "Competition rounds in ${param.seasonName} for ${param.competitionName}",
        bottomBar = {
            BottomBarWithButtons(
                onExport(),
                onImport(),
                addButtonSettings { onAdd() }
            )
        },
        states = persistentListOf(state)
    ) { paddingValues ->
        val values = state.values().filter { it.competitionId == param.competitionId }
            .sortedBy { it.round }.toImmutableList()
        LazyVerticalGrid(
            columns = WeightedIconGridCells(4, 1, 2, 2, 2),
            Modifier.padding(paddingValues),
            gridState
        ) {
            viewTextItems(listOf("Round", "Description", "Week", "Optional"))
            item { Icon(Blank, "") }
            item { Icon(Blank, "") }
            item { Icon(Blank, "") }
            item { Icon(Blank, "") }
                for (it in values) {
                    viewTextItems(listOf(it.round.toString(), it.description, DayDate(it.week).toString()))
                    item { Checkbox(checked = it.optional, onCheckedChange = null, enabled = false) }
                    item {
                        ClickableIcon(Icons.Filled.GridView, "Show fixtures") {
                            onShowFixtures(it)
                        }
                    }
                    item {
                        ClickableIconOld(Icons.Filled.Calculate, "Calculate fixtures") {
                            onCalculateFixtures(it.round)
                        }
                    }
                    editButton {
                        onEdit(it)
                    }
                    deleteButton(it != values[values.lastIndex]) {
                        onDelete(it)
                    }
                }
        }
    }
}

@Composable
fun SeasonCompetitionRoundEditScreen(
    param: SeasonCompetitionParam,
    competitionRound: SeasonCompetitionRound? = null
) {
    val viewModel: SeasonCompetitionRoundViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val seasonCompetitionViewModel: SeasonCompetitionViewModel =
        koinViewModel { parametersOf(param.seasonId, param.competitionId) }

    val state by viewModel.getState().collectAsStateWithLifecycle()
    val seasonCompetitionState by seasonCompetitionViewModel.getState().collectAsStateWithLifecycle()

    val rounds = getRounds(state.values())
    var description by remember { mutableStateOf(competitionRound?.description ?: "") }
    var week by remember { mutableIntStateOf(competitionRound?.week ?: 0) }
    var optional by remember { mutableStateOf(competitionRound?.optional ?: false) }
    var validRound by remember { mutableStateOf(true) }
    var round by remember(rounds) {
        mutableStateOf(
            competitionRound?.round ?: (if (rounds.isEmpty()) 1.toShort() else (rounds.max() + 1).toShort())
        )
    }

    ViewCommon(
        if (competitionRound == null) "Add Competition round" else "Edit Competition round",
        bottomBar = {
            BottomBarWithButton(enabled = description.isNotEmpty() && validRound) {
                viewModel.save(competitionRound, round, description, week, optional)
            }
        },
        confirm = { description.isNotEmpty() && validRound },
        confirmAction = {
            viewModel.save(competitionRound, round, description, week, optional)
            appNavigator.goBack()
        },
        states = persistentListOf(state, seasonCompetitionState)
    ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(4), Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Round", "Description", "Week", "Optional"))
            item {
                ReadonlyViewText(value = round.toString())
            }
            item { ViewTextField(description) { description = it } }
            item {
                DatePickerView(week,
                    Modifier,
                    { utcMs ->
                        val firstComp = seasonCompetitionState.values().firstOrNull()
                        if (firstComp != null) isMondayIn(firstComp, utcMs) else false
                    }) {
                    week = it
                }
            }
            item { Checkbox(checked = optional, onCheckedChange = { optional = it }) }
        }
    }
}

internal enum class FixtureResult(var display: String) {
    UN_PLAYED(""), HOME_WIN("Home win"), AWAY_WIN(
        "Away win"
    )
}

internal fun getFixtures(
    allFixtures: List<SeasonCupFixtureView>,
    round: Short,
    filterTeamCategory: String,
    fixturesById: Map<Long, SeasonCupFixtureView>,
    fixtureConsumer: (String, String, String, Short, Long, Boolean) -> Unit
) {
    allFixtures
        .filter { it.round == round && (filterTeamCategory.isBlank() || filterTeamCategory == it.teamCategoryName) }
        .forEach { fixture ->
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
               isPending(fixture, fixturesById)
            )
        }
}

private fun isPending(fixture: SeasonCupFixtureView, fixturesById: Map<Long, SeasonCupFixtureView>): Boolean {
    if (fixture.result > 0) {
        return false
    }
    return fixture.homePending > 0 && fixturesById[fixture.homePending]?.result!! == 0.toShort() ||
            fixture.awayPending > 0 && fixturesById[fixture.awayPending]?.result!! == 0.toShort()
}

internal fun teamDescription(
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
                previousFixture.homePending,
                previousFixture.homeAssociation,
                previousFixture.homeTeamNumber
            )
            val awayTeam = teamDescription(
                fixturesById,
                previousFixture.awayPending,
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

@Suppress("ParamsComparedByRef")
@Composable
fun SeasonCupFixtureScreen(param: SeasonCompetitionParam, competitionRound: SeasonCompetitionRound) {
    val viewModel: SeasonCompCupFixtureViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()

    val state by viewModel.getState().collectAsStateWithLifecycle()
    val teamCategoryState by teamCategoryViewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val edits = remember { mutableStateMapOf<Long, Short>() }
    var isLocked by remember { mutableStateOf(true) }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""
    val filterTeamCategory = remember { mutableStateOf("") }
    val fixturesById = state.values().associateBy { it.id }
    val withTeamCategory = filterTeamCategory.value.isBlank()

    fun getFixtures(fixtureConsumer: (String, String, String, Short, Long, Boolean) -> Unit) {
        getFixtures(state.values(), competitionRound.round, filterTeamCategory.value, fixturesById, fixtureConsumer)
    }

    ViewCommon(
        "${param.seasonName} ${param.competitionName} Round ${competitionRound.round} Fixtures",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Export", edits.isEmpty()) {
                    coroutineScope.launch {
                        val file = FileKit.openFileSaver(suggestedName = "cupFixtures", defaultExtension = "csv")
                        val sink = file?.sink(append = false)?.buffered()

                        sink.use { bufferedSink ->
                            if (withTeamCategory) {
                                bufferedSink?.writeString("Team Category,")
                            }
                            bufferedSink?.writeString("Home,Away\n")
                            getFixtures { teamCategoryName, home, away, _, _, _ ->
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
        },
        states = persistentListOf(state, teamCategoryState)
    ) { paddingValues ->
        val teamCategoryList = listOf("") + teamCategoryState.values().map { it.name }
        val columns = if (withTeamCategory) 4 else 3

        LazyVerticalGrid(columns = GridCells.Fixed(columns), Modifier.padding(paddingValues)) {
            item(span = { GridItemSpan(columns) }) {
                DropdownList(
                    teamCategoryList.toImmutableList(),
                    0,
                    label = "Filter Team Category"
                ) { filterTeamCategory.value = teamCategoryList[it] }
            }
            if (withTeamCategory) {
                item { ViewText("Team Category") }
            }
            viewTextItems(listOf("Home", "Away", "Winner"))
            getFixtures {
                    teamCategoryName, home, away, result, fixtureId, blankAwayAssociation ->
                if (withTeamCategory) {
                    item { ViewText(teamCategoryName) }
                }
                viewTextItems(listOf(home, away))
                item {
                    DropdownList(
                        itemList = FixtureResult.entries.map { it.display }.toImmutableList(),
                        selectedIndex = edits[fixtureId]?.toInt() ?: result.toInt(),
                        isLocked = { isLocked || blankAwayAssociation || result != 0.toShort() },
                    ) { edits[fixtureId] = it.toShort() }
                }
            }
        }
    }
}

private fun getRounds(data: List<SeasonCompetitionRound>): Set<Short> =
    data.map { it.round }.toSet()

data class CupFixtureTeams(
    val homeAssociation: AssociationId,
    val homeTeamNumber: TeamNumber,
    val awayAssociation: AssociationId,
    val awayTeamNumber: TeamNumber
)

internal suspend fun calcCupFixtures(
    seasonId: SeasonId,
    competitionId: CompetitionId,
    round: Short,
    seasonTeamDao: SeasonTeamDao = inject<SeasonTeamDao>(SeasonTeamDao::class.java).value,
    dao: SeasonCupFixtureDao = inject<SeasonCupFixtureDao>(SeasonCupFixtureDao::class.java).value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>(TeamCategoryDao::class.java).value
) {
    dao.deleteByRound(seasonId, competitionId, round)

    val teamCategories = teamCategoryDao.get()
    for (teamCategory in teamCategories) {
        if (round == 1.toShort()) {
            val competitionTeams =
                seasonTeamDao.getTeams(seasonId, competitionId, teamCategory.id)
                    .flatMap { seasonTeam ->
                        (0..<seasonTeam.count).map { teamNumber ->
                            Pair(seasonTeam.associationId, teamNumber.toShort())
                        }
                    }.shuffled(Random(System.currentTimeMillis()))
            val games = calculateGameCount(competitionTeams.size)

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
                            awayTeamNumber = it.awayTeamNumber,
                            result = (if (it.awayAssociation > 0) 0 else 1).toShort()
                        )
                    )
                }
        } else {
            val previousRoundFixtures =
                dao.getByRound(seasonId, competitionId, teamCategory.id, (round - 1).toShort())
                    .shuffled(Random(System.currentTimeMillis()))
            if (previousRoundFixtures.size > 1) {
            for (i in previousRoundFixtures.indices step 2) {
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

internal fun calculateGameCount(competitionTeamCount: Int): Int =
    roundUpToNextPowerOfTwo(competitionTeamCount) / 2

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

internal fun toDataFrame(
    records: List<SeasonCompetitionRound>,
    seasonCompViews: List<SeasonCompView>,
    competitions: List<Competition>
): DataFrame<SeasonCompetitionRound> {
    val seasonCompViewsById = seasonCompViews.associateBy { it.seasonId }
    val competitionsById = competitions.associateBy { it.id }
    return records.toDataFrame {
        SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
        COMPETITION_NAME from { competitionsById[it.competitionId]?.name }
        ROUND from { it.round }
        DESCRIPTION from { it.description }
        WEEK from { it.week }
        OPTIONAL from { it.optional }
    }
}

internal fun toSeasonCompetitionRound(
    row: DataRow<Any?>,
    seasonId: SeasonId,
    competitionId: CompetitionId
): SeasonCompetitionRound =
    SeasonCompetitionRound(
        seasonId,
        competitionId,
        short(row[ROUND]),
        string(row[DESCRIPTION]),
        int(row[WEEK]),
        boolean(row[OPTIONAL])
    )
