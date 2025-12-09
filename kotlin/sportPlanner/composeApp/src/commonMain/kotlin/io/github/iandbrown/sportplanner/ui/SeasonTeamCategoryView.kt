package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.ryinex.kotlin.datatable.data.DataTable
import com.ryinex.kotlin.datatable.data.DataTableConfig
import com.ryinex.kotlin.datatable.data.composable
import com.ryinex.kotlin.datatable.data.setList
import com.ryinex.kotlin.datatable.views.DataTableView
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import org.koin.compose.koinInject
import androidx.compose.runtime.LaunchedEffect
import com.ryinex.kotlin.datatable.data.text

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

    ViewCommon(MergedState(state.value, teamCategoryState.value), navController, "Season: ${param.seasonName} Competition: ${param.competitionName}", {
            FloatingActionButton(
                onClick = {
                    if (isLocked == EditorState.DIRTY) {
                        coroutineScope.launch {
                            save(viewModel, param, teamCategoryList, gameStructureStates, lockedStates)
                        }
                    }
                    isLocked = isLocked.onClick()
                },
                content = {
                    ViewText(isLocked.display)
                }
            )
        },
        "Return to Seasons screen") { paddingValues ->
        teamCategoryList = teamCategoryState.value.data?.sortedBy { it.name.trim().uppercase() }!!
        val seasonTeamCategories = state.value.data?.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }
                ?.associateBy { it.teamCategoryId }
        for (teamCategory in teamCategoryList) {
            val seasonTeamCategory = seasonTeamCategories?.get(teamCategory.id)
            gameStructureStates += seasonTeamCategory?.games ?: 0
            lockedStates += seasonTeamCategory?.locked ?: false
        }
        Column(modifier = Modifier.padding(paddingValues), content = {
            TeamCategoryDataTable(
                teamCategoryList,
                { isLocked == EditorState.LOCKED },
                DataTableConfig.default(isIndexed = false),
                { index -> MatchStructures.entries[gameStructureStates[index].toInt()].ordinal },
                { index, state -> gameStructureStates[index] = state.toShort(); isLocked = EditorState.DIRTY },
                { index -> lockedStates[index] },
                { index, state -> lockedStates[index] = state; isLocked = EditorState.DIRTY }
            )
        })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamCategoryDataTable(
    teamCategoryList: List<TeamCategory>,
    isLocked: () -> Boolean,
    config: DataTableConfig,
    getMatch: (index: Int) -> Int,
    setMatch: (index: Int, state: Int) -> Unit,
    getLocked: (index: Int) -> Boolean,
    setLocked: (index: Int, state: Boolean) -> Unit
) {
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val table = remember {
        DataTable<TeamCategory>(config = config, scope = scope, lazyState = lazyState)
            .text(name = "Name", value = { _, item -> item.name })
            .composable(name = "Match Structure", content = { location, _ ->
                DropdownList(
                    MatchStructures.entries.map { it.display }.toList(),
                    getMatch(location.layoutRowIndex - 1),
                    isLocked = isLocked
                ) { setMatch(location.layoutRowIndex - 1, it) }
            })
            .composable(name = "Locked", content = { location, _ ->
                Checkbox(
                    checked = getLocked(location.layoutRowIndex - 1),
                    onCheckedChange = {
                        if (!isLocked()) {
                            setLocked(location.layoutRowIndex - 1, it)
                        }
                    }
                )
            })
            .setList(list = teamCategoryList, key = { _, item -> item.id })
    }

    LaunchedEffect(isLocked()) {
        table.enableInteractions(!isLocked())
    }

    LaunchedEffect(config) {
        table.setConfig(config)
    }

    DataTableView(table = table)
}
