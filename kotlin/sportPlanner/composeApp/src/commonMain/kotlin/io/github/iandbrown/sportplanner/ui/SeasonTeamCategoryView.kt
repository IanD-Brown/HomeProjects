package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import org.koin.compose.koinInject

class SeasonTeamCategoryViewModel : BaseViewModel<SeasonTeamCategoryDao, SeasonTeamCategory>() {
    override fun getDao(db: AppDatabase): SeasonTeamCategoryDao = db.getSeasonTeamCategoryDao()
}

@Composable
fun NavigateSeasonTeamCategory(navController: NavController, argument: String?) {
    if (argument != null && argument.startsWith("View&")) {
        SeasonTeamCategoryEditor(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
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
private fun SeasonTeamCategoryEditor(navController: NavController, param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamCategoryViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val teamCategoryViewModel: TeamCategoryViewModel = koinInject()
    val teamCategoryState = teamCategoryViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isLocked by remember { mutableStateOf(EditorState.LOCKED) }
    var teamCategoryList = listOf<TeamCategory>()
    val gameStructureStates = remember { mutableStateListOf<Short>() }
    val lockedStates = remember { mutableStateListOf<Boolean>() }

    ViewCommon(MergedState(state.value, teamCategoryState.value),
        navController,
        "Season: ${param.seasonName} Competition: ${param.competitionName}",
        {},
        "Return to Seasons screen",
        {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton(isLocked.display, Modifier.weight(1f)){
                    if (isLocked == EditorState.DIRTY) {
                        coroutineScope.launch {
                            save(viewModel, param, teamCategoryList, gameStructureStates, lockedStates)
                        }
                    }
                    isLocked = isLocked.onClick()
                }
            }
        }) { paddingValues ->
        teamCategoryList = teamCategoryState.value.data?.sortedBy { it.name.trim().uppercase() }!!
        val seasonTeamCategories = state.value.data?.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }
            ?.associateBy { it.teamCategoryId }
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Team Category") }
            item { ReadonlyViewText("Match Structure")}
            item { ReadonlyViewText("Locked")}
            val matchStructureNamesList = MatchStructures.entries.map { it.display }.toList()
            var index = 0
            for (teamCategory in teamCategoryList) {
                val seasonTeamCategory = seasonTeamCategories?.get(teamCategory.id)
                gameStructureStates += seasonTeamCategory?.games ?: 0
                lockedStates += seasonTeamCategory?.locked ?: false
                val itemIndex = index++
                item { ReadonlyViewText(teamCategory.name) }
                item {
                    DropdownList(
                        matchStructureNamesList,
                        gameStructureStates[itemIndex].toInt(),
                        isLocked = { isLocked == EditorState.LOCKED }) {
                            gameStructureStates[itemIndex] = it.toShort()
                            isLocked = EditorState.DIRTY
                        }
                }
                item {
                    Checkbox(
                        checked = lockedStates[itemIndex],
                        enabled = isLocked != EditorState.LOCKED,
                        onCheckedChange = {
                            lockedStates[itemIndex] = it
                            isLocked = EditorState.DIRTY
                        }
                    )
                }
            }
        }
    }
}

private suspend fun save(
    viewModel: SeasonTeamCategoryViewModel,
    param: SeasonCompetitionParam,
    teamCategoryList: List<TeamCategory>,
    gameStructureStates: SnapshotStateList<Short>,
    lockedStates: SnapshotStateList<Boolean>
) {
    teamCategoryList.forEachIndexed { index, item ->
        viewModel.insert(
            SeasonTeamCategory(
                seasonId = param.seasonId,
                competitionId = param.competitionId,
                teamCategoryId = item.id,
                games = gameStructureStates[index],
                locked = lockedStates[index]
            )
        )
    }
    gameStructureStates.clear()
    lockedStates.clear()
}
