package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.logic.INCOMPLETE
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class SeasonTeamCategoryViewModel(
    seasonId: SeasonId,
    competitionId: CompetitionId,
    dao: SeasonTeamCategoryDao
) : BaseCRUDViewModel<SeasonTeamCategoryDao, SeasonTeamCategory>(dao, { it.get(seasonId, competitionId) }) {

    fun saveCategories(
        param: SeasonCompetitionParam,
        teamCategoryList: List<TeamCategory>,
        gameStructureStates: Map<TeamCategoryId, Short>,
        lockedStates: Map<TeamCategoryId, Boolean>
    ) {
        teamCategoryList.forEach { item ->
            insert(
                SeasonTeamCategory(
                    seasonId = param.seasonId,
                    competitionId = param.competitionId,
                    teamCategoryId = item.id,
                    games = gameStructureStates[item.id] ?: 0,
                    locked = lockedStates[item.id] ?: false
                )
            )
        }
    }
}

enum class MatchStructures(val display: String) { NONE(""), SINGLE("Single"), HOME_AWAY("Home and away") }

private enum class EditorState(val display: String) {
    LOCKED("Edit"),
    EDITING(""),
    DIRTY("Save");

    fun onClick(): EditorState = when (this) {
        LOCKED -> EDITING
        EDITING -> LOCKED
        DIRTY -> LOCKED
    }
}

@Composable
fun SeasonTeamCategoryScreen(param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamCategoryViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()
    val seasonTeamViewModel: SeasonTeamViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val seasonFixtureViewModel: SeasonFixtureViewModel = koinViewModel { parametersOf(param.seasonId) }

    val state by viewModel.getState().collectAsStateWithLifecycle()
    val teamCategoryState by teamCategoryViewModel.getState().collectAsStateWithLifecycle()
    val seasonTeamState by seasonTeamViewModel.getState().collectAsStateWithLifecycle()
    val seasonFixtureState by seasonFixtureViewModel.getState().collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    SeasonTeamCategoryContent(
        param = param,
        state = state,
        teamCategoryState = teamCategoryState,
        seasonTeamState = seasonTeamState,
        seasonFixtureState = seasonFixtureState,
        onSave = { teamCategoryList, gameStructureStates, lockedStates ->
            viewModel.saveCategories(param, teamCategoryList, gameStructureStates, lockedStates)
        }
    )
}

@Composable
private fun SeasonTeamCategoryContent(
    param: SeasonCompetitionParam,
    state: ViewModelState<SeasonTeamCategory>,
    teamCategoryState: ViewModelState<TeamCategory>,
    seasonTeamState: ViewModelState<SeasonTeam>,
    seasonFixtureState: ViewModelState<SeasonFixtureView>,
    onSave: (List<TeamCategory>, Map<TeamCategoryId, Short>, Map<TeamCategoryId, Boolean>) -> Unit
) {
    var isLocked by remember { mutableStateOf(EditorState.LOCKED) }
    val gameStructureStates = remember { mutableStateMapOf<TeamCategoryId, Short>() }
    val lockedStates = remember { mutableStateMapOf<TeamCategoryId, Boolean>() }

    ViewCommon(
        "Season: ${param.seasonName} Competition: ${param.competitionName}",
        description = "Return to Seasons screen",
        bottomBar = {
            BottomBarWithButton(isLocked.display) {
                if (isLocked == EditorState.DIRTY) {
                    onSave(teamCategoryState.values(), gameStructureStates, lockedStates)
                }
                isLocked = isLocked.onClick()
            }
        },
        states = persistentListOf(state, teamCategoryState, seasonTeamState, seasonFixtureState)
    ) { paddingValues ->
        val currentCategories = state.values()
        val teamCategoryList = teamCategoryState.values().sortedBy { it.name.trim().uppercase() }
        val seasonTeams = seasonTeamState.values()
        val fixtures = seasonFixtureState.values()

        currentCategories.forEach {
            if (!gameStructureStates.contains(it.teamCategoryId)) {
                gameStructureStates[it.teamCategoryId] = it.games
            }
            if (!lockedStates.contains(it.teamCategoryId)) {
                lockedStates[it.teamCategoryId] = it.locked
            }
        }
        val teamCounts = mutableMapOf<TeamCategoryId, Int>()
        for (seasonTeam in seasonTeams) {
            teamCounts.merge(seasonTeam.teamCategoryId, seasonTeam.count.toInt()) { a, b -> a + b }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Team Category", "Team Count", "Match Structure", "Locked", "Incomplete"))
            val matchStructureNamesList = MatchStructures.entries.map { it.display }.toList()
            for (teamCategory in teamCategoryList) {
                viewTextItems(listOf(teamCategory.name, teamCounts[teamCategory.id]?.toString() ?: "0"))
                item {
                    DropdownList(
                        matchStructureNamesList.toImmutableList(),
                        gameStructureStates[teamCategory.id]?.toInt() ?: 0,
                        isLocked = { isLocked == EditorState.LOCKED }) {
                        gameStructureStates[teamCategory.id] = it.toShort()
                        isLocked = EditorState.DIRTY
                    }
                }
                item {
                    Checkbox(
                        checked = lockedStates[teamCategory.id] ?: false,
                        enabled = isLocked != EditorState.LOCKED,
                        onCheckedChange = {
                            lockedStates[teamCategory.id] = it
                            isLocked = EditorState.DIRTY
                        }
                    )
                }
                item {
                    Checkbox(checked = fixtures
                        .filter { it.teamCategoryId == teamCategory.id }
                        .any { INCOMPLETE == it.message },
                        enabled = false, onCheckedChange = {})
                }
            }
        }
    }
}
