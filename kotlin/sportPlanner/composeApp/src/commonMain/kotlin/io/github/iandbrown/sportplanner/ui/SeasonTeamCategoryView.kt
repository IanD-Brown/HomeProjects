package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.logic.INCOMPLETE
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class SeasonTeamCategoryViewModel(seasonId : SeasonId,
                                  competitionId : CompetitionId,
                                  dao: SeasonTeamCategoryDao) :
    BaseSeasonCompCRUDViewModel<SeasonTeamCategoryDao, SeasonTeamCategory>(seasonId, competitionId, dao)

@Composable
fun NavigateSeasonTeamCategory(argument: String?) {
    if (argument != null && argument.startsWith("View&")) {
        SeasonTeamCategoryEditor(Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
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

@Suppress("ParamsComparedByRef")
@Composable
private fun SeasonTeamCategoryEditor(param: SeasonCompetitionParam,
                                     viewModel: SeasonTeamCategoryViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) },
                                     teamCategoryViewModel: TeamCategoryViewModel = koinViewModel(),
                                     seasonTeamViewModel: SeasonTeamViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) },
                                     seasonFixtureViewModel: SeasonFixtureViewModel = koinViewModel { parametersOf(param.seasonId) }
) {
    val state = viewModel.getState().collectAsState()
    val teamCategoryState = teamCategoryViewModel.getState().collectAsState()
    val seasonTeamState = seasonTeamViewModel.getState().collectAsState()
    val seasonFixtureState = seasonFixtureViewModel.getState().collectAsState()
    var isLocked by remember { mutableStateOf(EditorState.LOCKED) }
    var teamCategoryList = listOf<TeamCategory>()
    val gameStructureStates = remember { mutableStateMapOf<TeamCategoryId, Short>() }
    val lockedStates = remember { mutableStateMapOf<TeamCategoryId, Boolean>() }

    ViewCommon(
        "Season: ${param.seasonName} Competition: ${param.competitionName}",
        description = "Return to Seasons screen",
        bottomBar = {
            BottomBarWithButton(isLocked.display) {
                if (isLocked == EditorState.DIRTY) {
                    save(viewModel, param, teamCategoryList, gameStructureStates, lockedStates)
                }
                isLocked = isLocked.onClick()
            }
        },
        states = persistentListOf(state.value, teamCategoryState.value, seasonTeamState.value, seasonFixtureState.value)) { paddingValues ->
            state.values().forEach {
                if (!gameStructureStates.contains(it.teamCategoryId)) {
                    gameStructureStates[it.teamCategoryId] = it.games
                }
                if (!lockedStates.contains(it.teamCategoryId)) {
                    lockedStates[it.teamCategoryId] = it.locked
                }
            }
            val teamCounts = mutableMapOf<TeamCategoryId, Int>()
            for (seasonTeam in seasonTeamState.values()) {
                teamCounts.merge(seasonTeam.teamCategoryId, seasonTeam.count.toInt()) { a, b -> a + b }
            }

            teamCategoryList = teamCategoryState.values().sortedBy { it.name.trim().uppercase() }
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
                        Checkbox(checked = seasonFixtureState.values()
                            .filter { it.teamCategoryId == teamCategory.id }
                            .any { INCOMPLETE.equals(it.message) },
                            enabled = false, onCheckedChange = {})
                    }
                }
            }
        }
}

private fun save(
    viewModel: SeasonTeamCategoryViewModel,
    param: SeasonCompetitionParam,
    teamCategoryList: List<TeamCategory>,
    gameStructureStates: Map<TeamCategoryId, Short>,
    lockedStates: Map<TeamCategoryId, Boolean>
) {
    teamCategoryList.forEach { item ->
        viewModel.insert(
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
