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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.JoinType
import org.jetbrains.kotlinx.dataframe.api.join
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.inject

class SeasonViewModel(dao: SeasonDao) : BaseConfigCRUDViewModel<SeasonDao, Season>(dao) {
    fun saveCompetitions(name: String,
                         competitions: List<Competition>,
                         startDates: SnapshotStateMap<CompetitionId, Int>,
                         endDates: SnapshotStateMap<CompetitionId, Int>,
                         seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>(SeasonCompetitionDao::class.java).value) {
        viewModelScope.launch {
            val seasonId = dao.getSeasonId(name.trim())!!
            for (competition in competitions) {
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

// Used when viewing all seasons (SeasonListView) therefore no seasonId argument
class SeasonCompViewModel(dao: SeasonCompViewDao) :
    BaseConfigReadViewModel<SeasonCompViewDao, SeasonCompView>(dao) {
    fun deleteSeason(seasonId : SeasonId) {
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

private val editor : Editors = Editors.SEASONS

private const val ASSOCIATION_NAME = "AssociationName"
private const val COMPETITION_NAME = "CompetitionName"
private const val COUNT = "Count"
private const val DESCRIPTION = "Description"
private const val END_DATE = "EndDate"
private const val GAMES = "Games"
private const val LOCKED = "Locked"
private const val OPTIONAL = "Optional"
private const val ROUND = "Round"
private const val SEASON_BREAK_NAME = "SeasonBreakName"
private const val SEASON_NAME = "SeasonName"
private const val START_DATE = "StartDate"
private const val TEAM_CATEGORY_NAME = "TeamCategory"
private const val TYPE = "Type"
private const val WEEK = "Week"

private enum class DataFrameTypes {SEASON_COMP, BREAK, TEAM, MATCH, ROUND}
@Serializable
data class SeasonCompetitionParam(val seasonId : SeasonId, val seasonName : String, val competitionId : CompetitionId, val competitionName : String)

@Composable
fun NavigateSeason(argument: String?) {
    when (argument) {
        "View" -> SeasonListView()
        "Add" -> SeasonEditor()
        else -> SeasonEditor(Json.decodeFromString<Season>(argument!!))
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun SeasonListView(seasonCompViewModel: SeasonCompViewModel = koinViewModel(),
                           seasonViewModel: SeasonViewModel = koinViewModel()) {
    val seasonCompViewState = seasonCompViewModel.getState().collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Seasons",
        bottomBar = {
            BottomBarWithButtons(
            exportButtonSettings(coroutineScope, "seasons") {
                    toDataFrame(seasonCompViewState.values(),
                        inject<SeasonBreakDao>(SeasonBreakDao::class.java).value.getAll(),
                        inject<SeasonTeamDao>(SeasonTeamDao::class.java).value.getAll(),
                        inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value.getAll(),
                        inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value.getAll(),
                        inject<CompetitionDao>(CompetitionDao::class.java).value.get(),
                        inject<AssociationDao>(AssociationDao::class.java).value.get(),
                        inject<TeamCategoryDao>(TeamCategoryDao::class.java).value.get(),).writeJson(it)
                },
                ButtonSettings(imageVector = Icons.Default.Upload) {
                    coroutineScope.launch {
                        tryTransaction({seasonViewModel.handleException(it)}, {
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
                },
                addButtonSettings { it.navigate(editor.addRoute()) }
            )
        },
        states = persistentListOf(seasonCompViewState.value)) { paddingValues ->
        var seasonId : Short? = null
        val surfaceColor = MaterialTheme.colorScheme.onSurface

        LazyVerticalGrid(WeightedIconGridCells(3, 1, 2), modifier = Modifier.padding(paddingValues)) {
            for (seasonCompView in seasonCompViewState.values()) {
                if (seasonCompView.seasonId != seasonId) {
                    viewTextItems(listOf(seasonCompView.seasonName,""))

                    clickableIcon(Icons.Default.Splitscreen, "manage season breaks", surfaceColor) {
                        Editors.SEASON_BREAK.viewRoute(Season(seasonCompView.seasonId, seasonCompView.seasonName))
                    }
                    editButton { editor.editRoute(Season(seasonCompView.seasonId, seasonCompView.seasonName)) }
                    deleteButton { seasonCompViewModel.deleteSeason(seasonCompView.seasonId) }

                    seasonId = seasonCompView.seasonId
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
                        Editors.SEASON_COMPETITION_ROUND.viewRoute(seasonCompetitionParamOf(seasonCompView))
                    }
                } else {
                    clickableIcon(Icons.Default.Accessibility, "manage teams", surfaceColor) {
                        Editors.SEASON_TEAM_CATEGORY.viewRoute(seasonCompetitionParamOf(seasonCompView))
                    }
                }
                clickableIcon(Icons.Default._123, "manage match structure", surfaceColor) {
                    Editors.SEASON_TEAMS.viewRoute(seasonCompetitionParamOf(seasonCompView))
                }
            }
        }
    }
}

internal fun toDataFrame(seasonCompViews: List<SeasonCompView>,
                         seasonBreaks: List<SeasonBreak>,
                         seasonTeams: List<SeasonTeam>,
                         seasonTeamCategories: List<SeasonTeamCategory>,
                         competitionRounds: List<SeasonCompetitionRound>,
                         competitions: List<Competition>,
                         associations: List<Association>,
                         teamCategories: List<TeamCategory>):
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
            SEASON_BREAK_NAME from {it.name}
            WEEK from {it.week}
        }, JoinType.Full).join(seasonTeams.toDataFrame {
            TYPE from {DataFrameTypes.TEAM.name}
            SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
            COMPETITION_NAME from { competitionsById[it.competitionId]?.name }
            ASSOCIATION_NAME from { associationsById[it.associationId]?.name }
            TEAM_CATEGORY_NAME from { teamCategoriesById[it.teamCategoryId]?.name }
            COUNT from {it.count}
        }, JoinType.Full).join(seasonTeamCategories.toDataFrame {
            TYPE from {DataFrameTypes.MATCH.name}
            SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
            COMPETITION_NAME from { competitionsById[it.competitionId]?.name }
            TEAM_CATEGORY_NAME from { teamCategoriesById[it.teamCategoryId]?.name }
            GAMES from {it.games}
            LOCKED from {it.locked}
        }, JoinType.Full).join(competitionRounds.toDataFrame {
            TYPE from {DataFrameTypes.ROUND.name}
            SEASON_NAME from { seasonCompViewsById[it.seasonId]?.seasonName }
            COMPETITION_NAME from { competitionsById[it.competitionId]?.name }
            ROUND from {it.round}
            DESCRIPTION from {it.description}
            WEEK from {it.week}
            OPTIONAL from {it.optional}
        }, JoinType.Full)
    }

internal suspend fun importRow(row: DataRow<Any?>,
                               seasonDao: SeasonDao = inject<SeasonDao>(SeasonDao::class.java).value,
                               seasonCompetitionDao: SeasonCompetitionDao = inject<SeasonCompetitionDao>(SeasonCompetitionDao::class.java).value,
                               seasonBreakDao: SeasonBreakDao = inject<SeasonBreakDao>(SeasonBreakDao::class.java).value,
                               seasonTeamDao: SeasonTeamDao = inject<SeasonTeamDao>(SeasonTeamDao::class.java).value,
                               seasonTeamCategoryDao: SeasonTeamCategoryDao = inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value,
                               seasonCompetitionRoundDao: SeasonCompetitionRoundDao = inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value,
                               competitionDao: CompetitionDao = inject<CompetitionDao>(CompetitionDao::class.java).value,
                               associationDao: AssociationDao = inject<AssociationDao>(AssociationDao::class.java).value,
                               teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>(TeamCategoryDao::class.java).value) {
    when (row[TYPE]) {
        DataFrameTypes.SEASON_COMP.name -> {
            if (seasonDao.getSeasonId(string(row[SEASON_NAME])) == null) {
                seasonDao.insert(Season(name = string(row[SEASON_NAME])))
            }
            seasonCompetitionDao.insert(SeasonCompetition(seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
                competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
                int(row[START_DATE]),
                int(row[END_DATE])))
        }
        DataFrameTypes.BREAK.name -> seasonBreakDao.insert(SeasonBreak(seasonId = seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
            name = string(row[SEASON_BREAK_NAME]), week = int(row[WEEK])))
        DataFrameTypes.TEAM.name -> seasonTeamDao.insert(SeasonTeam(seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
            competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
            associationDao.getByName(string(row[ASSOCIATION_NAME]))!!,
            teamCategoryDao.getByName(string(row[TEAM_CATEGORY_NAME]))!!,
            short(row[COUNT])))
        DataFrameTypes.MATCH.name -> seasonTeamCategoryDao.insert(SeasonTeamCategory(seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
            competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
            teamCategoryDao.getByName(string(row[TEAM_CATEGORY_NAME]))!!,
            short(row[GAMES]),
            boolean(row[LOCKED])))
        DataFrameTypes.ROUND.name -> seasonCompetitionRoundDao.insert(SeasonCompetitionRound(seasonDao.getSeasonId(string(row[SEASON_NAME]))!!,
            competitionDao.getByName(string(row[COMPETITION_NAME]))!!,
            short(row[ROUND]),
            string(row[DESCRIPTION]),
            int(row[WEEK]),
            boolean(row[OPTIONAL])))
    }
}

private fun boolean(cell: Any?) : Boolean =
    when (cell) {
        is Boolean -> cell
        is String -> cell.toBoolean()
        else -> false
    }

private fun string(cell: Any?): String =
    when (cell) {
        is String -> cell
        is Double -> cell.toString()
        is Char -> cell.toString()
        else -> ""
    }

private fun int(cell: Any?) : Int =
    when (cell) {
        is Int -> cell
        is String -> if (cell.isNotBlank()) cell.toInt() else 0
        else -> 0
    }
private fun short(cell: Any?) : Short =
    when (cell) {
        is Int -> cell.toShort()
        is String -> if (cell.isNotBlank()) cell.toShort() else 0
        else -> 0
    }

private fun seasonCompetitionParamOf(seasonCompView : SeasonCompView) : SeasonCompetitionParam =
    SeasonCompetitionParam(seasonCompView.seasonId, seasonCompView.seasonName, seasonCompView.competitionId, seasonCompView.competitionName)

@Suppress("ParamsComparedByRef")
@Composable
private fun SeasonEditor(season : Season? = null,
                         viewModel: SeasonViewModel = koinInject(),
                         seasonCompViewModel: SeasonCompViewModel = koinViewModel(),
                         competitionViewModel: CompetitionViewModel = koinViewModel()) {
    val seasonCompetitionState = seasonCompViewModel.getState().collectAsState()
    val competitionState = competitionViewModel.getState().collectAsState()
    var name by remember { mutableStateOf(season?.name ?: "") }
    val title = if (season == null) "Add Season" else "Edit Season"
    val startDates = remember { mutableStateMapOf<Short, Int>() }
    val endDates = remember { mutableStateMapOf<Short, Int>() }
    var dirty by remember { mutableStateOf(false) }

    ViewCommon(
        title,
        description = "Return to seasons",
        bottomBar = {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton(OK, Modifier.weight(1f), name.isNotBlank()) {
                    save(season, viewModel, name, competitionState.values(), startDates, endDates)
                    appNavController.popBackStack()
                }
            }
        },
        confirm = { dirty },
        confirmAction = { save(season, viewModel, name, competitionState.values(), startDates, endDates) },
        states = persistentListOf(competitionState.value)) { paddingValues ->
            val competitionList = competitionState.values().sortedBy { it.name.trim().uppercase() }
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
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText("Name:") }
                item { ViewTextField(value = name, onValueChange = {
                    name = it
                    dirty = checkDirty(season, name, seasonCompetitionState.values(), startDates, endDates)
                }) }
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
                                dayDate.isMonday() && dayDate.value() < (endDates[competition.id] ?: Integer.MAX_VALUE) }) {
                            startDates[competition.id] = it
                            dirty = checkDirty(season, name, seasonCompetitionState.values(), startDates, endDates)
                        }
                    }
                    item {
                        DatePickerView(
                            current = endDates[competition.id] ?: 0,
                            modifier = Modifier,
                            isSelectable = { val dayDate = DayDate(it)
                                dayDate.isSunday() && dayDate.value() > (startDates[competition.id] ?: 0) }) {
                            endDates[competition.id] = it
                            dirty = checkDirty(season, name, seasonCompetitionState.values(), startDates, endDates)
                        }
                    }
                }
            }
        }
}

private fun checkDirty(season: Season?, name: String, seasonCompetitionState: List<SeasonCompView>, startDates: SnapshotStateMap<CompetitionId, Int>, endDates: SnapshotStateMap<CompetitionId, Int>): Boolean =
    if (season == null) {
        name.isNotEmpty() || startDates.isNotEmpty() || endDates.isNotEmpty()
    } else {
        if (name != season.name) {
            true
        } else {
            var result = false
            for (current in seasonCompetitionState) {
                if ((startDates[current.competitionId] ?: 0) != current.startDate ||
                    (endDates[current.competitionId] ?: 0) != current.endDate) {
                    result = true
                    break
                }
            }
            result
        }
    }

private fun save(season : Season?,
                 viewModel: SeasonViewModel,
                 name: String,
                 competitionState: List<Competition>,
                 startDates: SnapshotStateMap<Short, Int>, endDates: SnapshotStateMap<Short, Int>) {
    if (season == null) {
        viewModel.insert(Season(name = name.trim()))
    } else {
        viewModel.update(Season(season.id, name.trim()))
    }
    viewModel.saveCompetitions(name, competitionState, startDates, endDates)
}
