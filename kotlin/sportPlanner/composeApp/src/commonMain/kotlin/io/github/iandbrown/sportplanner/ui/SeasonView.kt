package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled._123
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.AssociationDao
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonBreakDao
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.database.SeasonDao
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.logic.DayDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.JoinType
import org.jetbrains.kotlinx.dataframe.api.at
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.join
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.inject

class SeasonViewModel(dao: SeasonDao) : BaseCRUDViewModel<SeasonDao, Season>(dao, { it.get() }) {
    fun save(
        season: Season?,
        name: String,
        competitionState: List<Competition>,
        startDates: SnapshotStateMap<Short, Int>,
        endDates: SnapshotStateMap<Short, Int>,
        seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>(SeasonCompetitionDao::class.java).value
    ) {
        viewModelScope.launch {
            if (season == null) {
                insert(Season(name = name.trim()))
            } else {
                update(Season(season.id, name.trim()))
            }
            val seasonId = dao.getSeasonId(name.trim())!!
            for (competition in competitionState) {
                val seasonCompetition = SeasonCompetition(
                    seasonId = seasonId,
                    competitionId = competition.id,
                    startDate = startDates[competition.id] ?: 0,
                    endDate = endDates[competition.id] ?: 0
                )
                if (seasonCompetition.isValid()) {
                    seasonCompetitionDao.insert(seasonCompetition)
                }
            }
        }
    }
}

class SeasonCompViewModel(dao: SeasonCompViewDao) :
    BaseReadViewModel<SeasonCompViewDao, SeasonCompView>(dao, { it.get() }) {
    fun deleteSeason(seasonId: SeasonId) {
        viewModelScope.launch {
            try {
                dao.deleteSeason(seasonId)
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}

private const val ASSOCIATION_NAME = "AssociationName"
internal const val COMPETITION_NAME = "CompetitionName"
private const val COUNT = "Count"
internal const val DESCRIPTION = "Description"
private const val END_DATE = "EndDate"
private const val GAMES = "Games"
private const val LOCKED = "Locked"
internal const val OPTIONAL = "Optional"
internal const val ROUND = "Round"
private const val SEASON_BREAK_NAME = "SeasonBreakName"
internal const val SEASON_NAME = "SeasonName"
private const val START_DATE = "StartDate"
private const val TEAM_CATEGORY_NAME = "TeamCategory"
private const val TYPE = "Type"
internal const val WEEK = "Week"

private enum class DataFrameTypes { SEASON_COMP, BREAK, TEAM, MATCH, ROUND }

@Serializable
data class SeasonCompetitionParam(
    val seasonId: SeasonId,
    val seasonName: String,
    val competitionId: CompetitionId,
    val competitionName: String
)

@Composable
fun SeasonListScreen() {
    val seasonCompViewModel: SeasonCompViewModel = koinViewModel()
    val seasonViewModel: SeasonViewModel = koinViewModel()
    val seasonCompViewState by seasonCompViewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(seasonCompViewModel) {
        seasonCompViewModel.readAll()
        onPauseOrDispose { }
    }

    SeasonListContent(
        state = seasonCompViewState,
        onExport = {
            exportButtonSettings(coroutineScope, "seasons") {
                toDataFrame(
                    seasonCompViewState.values(),
                    inject<SeasonBreakDao>(SeasonBreakDao::class.java).value.getAll(),
                    inject<SeasonTeamDao>(SeasonTeamDao::class.java).value.getAll(),
                    inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value.getAll(),
                    inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value.getAll(),
                    inject<CompetitionDao>(CompetitionDao::class.java).value.get(),
                    inject<AssociationDao>(AssociationDao::class.java).value.get(),
                    inject<TeamCategoryDao>(TeamCategoryDao::class.java).value.get(),
                ).writeJson(it)
            }
        },
        onImport = {
            ButtonSettings(imageVector = Icons.Default.Upload) {
                coroutineScope.launch {
                    tryTransaction({ seasonViewModel.handleException(it) }, {
                        importFromFile(
                            "json",
                            {
                                val dataFrame = DataFrame.readJson(it)
                                seasonViewModel.dao.deleteAll()
                                dataFrame
                            })
                        { importRow(it) }
                    })
                    seasonCompViewModel.readAll()
                }
            }
        },
        onAdd = { appNavigator.navigate(Route.SeasonEdit(null)) },
        onManageBreaks = { appNavigator.navigate(Route.SeasonBreakList(it)) },
        onEdit = { appNavigator.navigate(Route.SeasonEdit(it)) },
        onDelete = { seasonCompViewModel.deleteSeason(it) },
        onManageRounds = { appNavigator.navigate(Route.SeasonCompetitionRoundList(it)) },
        onManageTeams = { appNavigator.navigate(Route.SeasonTeamCategory(it)) },
        onManageMatchStructure = { appNavigator.navigate(Route.SeasonTeams(it)) }
    )
}

@Composable
private fun SeasonListContent(
    state: ViewModelState<SeasonCompView>,
    onExport: () -> ButtonSettings,
    onImport: () -> ButtonSettings,
    onAdd: () -> Unit,
    onManageBreaks: (Season) -> Unit,
    onEdit: (Season) -> Unit,
    onDelete: (SeasonId) -> Unit,
    onManageRounds: (SeasonCompetitionParam) -> Unit,
    onManageTeams: (SeasonCompetitionParam) -> Unit,
    onManageMatchStructure: (SeasonCompetitionParam) -> Unit
) {
    ViewCommon(
        "Seasons",
        bottomBar = {
            BottomBarWithButtons(
                onExport(),
                onImport(),
                addButtonSettings { onAdd() }
            )
        },
        states = persistentListOf(state)
    ) { paddingValues ->
        var currentSeasonId: Short? = null
        val surfaceColor = MaterialTheme.colorScheme.onSurface
        val items = state.values()

        LazyVerticalGrid(WeightedIconGridCells(3, 1, 2), modifier = Modifier.padding(paddingValues)) {
            for (seasonCompView in items) {
                if (seasonCompView.seasonId != currentSeasonId) {
                    viewTextItems(listOf(seasonCompView.seasonName, ""))

                    clickableIcon(Icons.Default.Splitscreen, "manage season breaks", surfaceColor) {
                        onManageBreaks(Season(seasonCompView.seasonId, seasonCompView.seasonName))
                    }
                    editButton {
                        onEdit(Season(seasonCompView.seasonId, seasonCompView.seasonName))
                    }
                    deleteButton { onDelete(seasonCompView.seasonId) }

                    currentSeasonId = seasonCompView.seasonId
                }
                val startDate = DayDate(seasonCompView.startDate)
                val endDate = DayDate(seasonCompView.endDate)
                val join = if (startDate.isValid() || endDate.isValid()) "to" else ""
                viewTextItems(listOf(" * ${seasonCompView.competitionName}", "$startDate $join $endDate"))

                item {
                    Icon(Blank, "")
                }

                if (seasonCompView.competitionType == CompetitionTypes.KNOCK_OUT_CUP.ordinal.toShort()) {
                    clickableIcon(Icons.Default.Rotate90DegreesCcw, "manage season competition rounds", surfaceColor) {
                        onManageRounds(seasonCompetitionParamOf(seasonCompView))
                    }
                } else {
                    clickableIcon(Icons.Default.Accessibility, "manage teams", surfaceColor) {
                        onManageTeams(seasonCompetitionParamOf(seasonCompView))
                    }
                }
                clickableIcon(Icons.Default._123, "manage match structure", surfaceColor) {
                    onManageMatchStructure(seasonCompetitionParamOf(seasonCompView))
                }
            }
        }
    }
}

internal fun toDataFrame(
    seasonCompViews: List<SeasonCompView>,
    seasonBreaks: List<SeasonBreak>,
    seasonTeams: List<SeasonTeam>,
    seasonTeamCategories: List<SeasonTeamCategory>,
    competitionRounds: List<SeasonCompetitionRound>,
    competitions: List<Competition>,
    associations: List<Association>,
    teamCategories: List<TeamCategory>
):
        DataFrame<SeasonCompView> {
    val seasonCompViewsById = seasonCompViews.associateBy { it.seasonId }
    val competitionsById = competitions.associateBy { it.id }
    val associationsById = associations.associateBy { it.id }
    val teamCategoriesById = teamCategories.associateBy { it.id }
    return seasonCompViews.toDataFrame {
        TYPE from { DataFrameTypes.SEASON_COMP.name }
        SEASON_NAME from { it.seasonName }
        COMPETITION_NAME from { it.competitionName }
        START_DATE from { it.startDate }
        END_DATE from { it.endDate }
    }.join(seasonBreaks.toDataFrame {
        TYPE from { DataFrameTypes.BREAK.name }
        SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
        SEASON_BREAK_NAME from { it.name }
        WEEK from { it.week }
    }, JoinType.Full).join(seasonTeams.toDataFrame {
        TYPE from { DataFrameTypes.TEAM.name }
        SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
        COMPETITION_NAME from { competitionsById[it.competitionId]?.name }
        ASSOCIATION_NAME from { associationsById[it.associationId]?.name }
        TEAM_CATEGORY_NAME from { teamCategoriesById[it.teamCategoryId]?.name }
        COUNT from { it.count }
    }, JoinType.Full).join(seasonTeamCategories.toDataFrame {
        TYPE from { DataFrameTypes.MATCH.name }
        SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
        COMPETITION_NAME from { competitionsById[it.competitionId]?.name }
        TEAM_CATEGORY_NAME from { teamCategoriesById[it.teamCategoryId]?.name }
        GAMES from { it.games }
        LOCKED from { it.locked }
    }, JoinType.Full).join(
        toDataFrame(competitionRounds, seasonCompViews, competitions)
            .insert(TYPE) { DataFrameTypes.ROUND.name }.at(0), JoinType.Full
    )
}

internal suspend fun importRow(
    row: DataRow<Any?>,
    seasonDao: SeasonDao = inject<SeasonDao>(SeasonDao::class.java).value,
    seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>(SeasonCompetitionDao::class.java).value,
    seasonBreakDao: SeasonBreakDao = inject<SeasonBreakDao>(SeasonBreakDao::class.java).value,
    seasonTeamDao: SeasonTeamDao = inject<SeasonTeamDao>(SeasonTeamDao::class.java).value,
    seasonTeamCategoryDao: SeasonTeamCategoryDao = inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value,
    seasonCompetitionRoundDao: SeasonCompetitionRoundDao = inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value,
    competitionDao: CompetitionDao = inject<CompetitionDao>(CompetitionDao::class.java).value,
    associationDao: AssociationDao = inject<AssociationDao>(AssociationDao::class.java).value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>(TeamCategoryDao::class.java).value
) {
    when (row[TYPE]) {
        DataFrameTypes.SEASON_COMP.name -> {
            if (seasonDao.getSeasonId(string(row[SEASON_NAME])) == null) {
                seasonDao.insert(Season(name = string(row[SEASON_NAME])))
            }
            seasonCompetitionDao.insert(
                SeasonCompetition(
                    seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
                    competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
                    int(row[START_DATE]),
                    int(row[END_DATE])
                )
            )
        }

        DataFrameTypes.BREAK.name -> seasonBreakDao.insert(
            SeasonBreak(
                seasonId = seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
                name = string(row[SEASON_BREAK_NAME]), week = int(row[WEEK])
            )
        )

        DataFrameTypes.TEAM.name -> seasonTeamDao.insert(
            SeasonTeam(
                seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
                competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
                associationDao.getByName(string(row[ASSOCIATION_NAME]))!!,
                teamCategoryDao.getByName(string(row[TEAM_CATEGORY_NAME]))!!,
                short(row[COUNT])
            )
        )

        DataFrameTypes.MATCH.name -> seasonTeamCategoryDao.insert(
            SeasonTeamCategory(
                seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
                competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
                teamCategoryDao.getByName(string(row[TEAM_CATEGORY_NAME]))!!,
                short(row[GAMES]),
                boolean(row[LOCKED])
            )
        )

        DataFrameTypes.ROUND.name -> seasonCompetitionRoundDao.insert(
            toSeasonCompetitionRound(
                row,
                seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
                competitionDao.getByName(string(row[COMPETITION_NAME]))!!
            )
        )
    }
}

internal fun boolean(cell: Any?): Boolean =
    when (cell) {
        is Boolean -> cell
        is String -> cell.toBoolean()
        else -> false
    }

internal fun string(cell: Any?): String =
    when (cell) {
        is String -> cell
        is Double -> cell.toString()
        is Char -> cell.toString()
        else -> ""
    }

internal fun int(cell: Any?): Int =
    when (cell) {
        is Int -> cell
        is String -> if (cell.isNotBlank()) cell.toInt() else 0
        else -> 0
    }

internal fun short(cell: Any?): Short =
    when (cell) {
        is Int -> cell.toShort()
        is String -> if (cell.isNotBlank()) cell.toShort() else 0
        else -> 0
    }

private fun seasonCompetitionParamOf(seasonCompView: SeasonCompView): SeasonCompetitionParam =
    SeasonCompetitionParam(
        seasonCompView.seasonId,
        seasonCompView.seasonName,
        seasonCompView.competitionId,
        seasonCompView.competitionName
    )

@Composable
fun SeasonEditScreen(season: Season? = null) {
    val viewModel: SeasonViewModel = koinViewModel()
    val seasonCompViewModel: SeasonCompViewModel = koinViewModel()
    val competitionViewModel: CompetitionViewModel = koinViewModel()

    val seasonCompetitionState by seasonCompViewModel.getState().collectAsStateWithLifecycle()
    val competitionState by competitionViewModel.getState().collectAsStateWithLifecycle()

    SeasonEditContent(
        season = season,
        seasonCompetitionState = seasonCompetitionState,
        competitionState = competitionState,
        onSave = { name, competitions, startDates, endDates ->
            viewModel.save(season, name, competitions, startDates, endDates)
            appNavigator.goBack()
        },
        onConfirmSave = { name, competitions, startDates, endDates ->
            viewModel.save(season, name, competitions, startDates, endDates)
        }
    )
}

@Composable
private fun SeasonEditContent(
    season: Season?,
    seasonCompetitionState: ViewModelState<SeasonCompView>,
    competitionState: ViewModelState<Competition>,
    onSave: (String, List<Competition>, SnapshotStateMap<Short, Int>, SnapshotStateMap<Short, Int>) -> Unit,
    onConfirmSave: (String, List<Competition>, SnapshotStateMap<Short, Int>, SnapshotStateMap<Short, Int>) -> Unit
) {
    var name by remember { mutableStateOf(season?.name ?: "") }
    val startDates = remember { mutableStateMapOf<Short, Int>() }
    val endDates = remember { mutableStateMapOf<Short, Int>() }
    var dirty by remember { mutableStateOf(false) }

    val competitionList = competitionState.values().sortedBy { it.name.trim().uppercase() }

    remember(season, seasonCompetitionState) {
        if (season != null) {
            for (current in seasonCompetitionState.values()) {
                if (current.startDate > 0) {
                    startDates[current.competitionId] = current.startDate
                }
                if (current.endDate > 0) {
                    endDates[current.competitionId] = current.endDate
                }
            }
        }
        true
    }

    ViewCommon(
        if (season == null) "Add Season" else "Edit Season",
        description = "Return to seasons",
        bottomBar = {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton(OK, Modifier.weight(1f), name.isNotBlank()) {
                    onSave(name, competitionList, startDates, endDates)
                }
            }
        },
        confirm = { dirty },
        confirmAction = { onConfirmSave(name, competitionList, startDates, endDates) },
        states = persistentListOf(competitionState, seasonCompetitionState)
    ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Name:") }
            item {
                ViewTextField(value = name, onValueChange = {
                    name = it
                    dirty = checkDirty(season, name, seasonCompetitionState.values(), startDates, endDates)
                })
            }
            item { ReadonlyViewText("") }
            item { ReadonlyViewText("Competition") }
            item { ReadonlyViewText("Start") }
            item { ReadonlyViewText("End") }
            for (competition in competitionList) {
                item { ReadonlyViewText(competition.name) }
                item {
                    DatePickerView(
                        current = startDates[competition.id] ?: 0,
                        modifier = Modifier,
                        isSelectable = {
                            val dayDate = DayDate(it)
                            dayDate.isMonday() && dayDate.value() < (endDates[competition.id] ?: Integer.MAX_VALUE)
                        }) {
                        startDates[competition.id] = it
                        dirty = checkDirty(season, name, seasonCompetitionState.values(), startDates, endDates)
                    }
                }
                item {
                    DatePickerView(
                        current = endDates[competition.id] ?: 0,
                        modifier = Modifier,
                        isSelectable = {
                            val dayDate = DayDate(it)
                            dayDate.isSunday() && dayDate.value() > (startDates[competition.id] ?: 0)
                        }) {
                        endDates[competition.id] = it
                        dirty = checkDirty(season, name, seasonCompetitionState.values(), startDates, endDates)
                    }
                }
            }
        }
    }
}

private fun checkDirty(
    season: Season?,
    name: String,
    seasonCompetitionState: List<SeasonCompView>,
    startDates: SnapshotStateMap<CompetitionId, Int>,
    endDates: SnapshotStateMap<CompetitionId, Int>
): Boolean =
    if (season == null) {
        name.isNotEmpty() || startDates.isNotEmpty() || endDates.isNotEmpty()
    } else {
        if (name != season.name) {
            true
        } else {
            var result = false
            for (current in seasonCompetitionState) {
                if ((startDates[current.competitionId] ?: 0) != current.startDate ||
                    (endDates[current.competitionId] ?: 0) != current.endDate
                ) {
                    result = true
                    break
                }
            }
            result
        }
    }
