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
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class SeasonTeamCategoryViewModel(seasonId : SeasonId, competitionId : CompetitionId) :
    BaseSeasonCompCRUDViewModel<SeasonTeamCategoryDao, SeasonTeamCategory>(
        seasonId,
        competitionId,
        inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value
    )

@Composable
fun NavigateSeasonTeamCategory(argument: String?) {
    if (argument != null && argument.startsWith("View&")) {
        SeasonTeamCategoryEditor(Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
    }
}

private enum class MatchStructures(val display: String) { NONE(""), SINGLE("Single"), HOME_AWAY("Home and away") }
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
private fun SeasonTeamCategoryEditor(param: SeasonCompetitionParam) {
    val seasonParams = parametersOf(param.seasonId, param.competitionId)
    val viewModel: SeasonTeamCategoryViewModel = koinInject {seasonParams}
    val state = viewModel.uiState.collectAsState()
    val teamCategoryViewModel: TeamCategoryViewModel = koinInject()
    val teamCategoryState = teamCategoryViewModel.uiState.collectAsState(emptyList())
    val seasonTeamViewModel: SeasonTeamViewModel = koinInject { parametersOf(param.seasonId, param.competitionId) }
    val seasonTeamState = seasonTeamViewModel.uiState.collectAsState(emptyList())
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
        content = { paddingValues ->
            state.value.forEach {
                if (!gameStructureStates.contains(it.teamCategoryId)) {
                    gameStructureStates[it.teamCategoryId] = it.games
                }
                if (!lockedStates.contains(it.teamCategoryId)) {
                    lockedStates[it.teamCategoryId] = it.locked
                }
            }
            val teamCounts = mutableMapOf<TeamCategoryId, Int>()
            for (seasonTeam in seasonTeamState.value) {
                teamCounts[seasonTeam.teamCategoryId] = teamCounts.getOrDefault(seasonTeam.teamCategoryId, 0) + 1
            }

            teamCategoryList = teamCategoryState.value.sortedBy { it.name.trim().uppercase() }
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText("Team Category") }
                item { ReadonlyViewText("Team Count") }
                item { ReadonlyViewText("Match Structure")}
                item { ReadonlyViewText("Locked")}
                val matchStructureNamesList = MatchStructures.entries.map { it.display }.toList()
                for (teamCategory in teamCategoryList) {
                    item { ReadonlyViewText(teamCategory.name) }
                    item { ReadonlyViewText(teamCounts[teamCategory.id].toString()) }
                    item {
                        DropdownList(
                            matchStructureNamesList,
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
                }
            }
        })
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
