package org.idb.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ryinex.kotlin.datatable.data.DataTable
import com.ryinex.kotlin.datatable.data.DataTableConfig
import com.ryinex.kotlin.datatable.data.composable
import com.ryinex.kotlin.datatable.data.setList
import com.ryinex.kotlin.datatable.views.DataTableView
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.idb.database.AppDatabase
import org.idb.database.SeasonTeamCategory
import org.idb.database.SeasonTeamCategoryDao
import org.idb.database.TeamCategory
import org.koin.compose.koinInject

class SeasonTeamCategoryViewModel : BaseViewModel<SeasonTeamCategoryDao, SeasonTeamCategory>() {
    override fun getDao(db: AppDatabase): SeasonTeamCategoryDao = db.getSeasonTeamCategoryDao()
}

@Composable
fun navigateSeasonTeamCategory(navController : NavController, argument : String?) {
    if (argument != null && argument.startsWith("View&")) {
        seasonTeamCategoryEditor(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
    }
}

private enum class MatchStructures(val display : String) {SINGLE("Single"), HOME_AWAY("Home and away")}

@Composable
@Preview
private fun seasonTeamCategoryEditor(navController: NavController, param : SeasonCompetitionParam) {
    val viewModel : SeasonTeamCategoryViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val teamCategoryViewModel : TeamCategoryViewModel = koinInject()
    val teamCategoryState = teamCategoryViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isLocked by remember { mutableStateOf(true) }
    var buttonText by remember {mutableStateOf("Edit")}
    val mergedState = object : BaseUiState {
        override fun loadingInProgress(): Boolean = state.value.isLoading || teamCategoryState.value.isLoading

        override fun hasData(): Boolean = state.value.data != null && teamCategoryState.value.data != null
    }
    var teamCategoryList = listOf<TeamCategory>()
    val gameStructureStates = remember {mutableStateListOf<Short>()}
    val lockedStates = remember { mutableStateListOf<Boolean>() }

    viewCommon(mergedState, navController, "Season: ${param.seasonName} Competition: ${param.competitionName}", {
        FloatingActionButton(onClick = {
            if (!isLocked) {
                coroutineScope.launch {
                    save(viewModel, param, teamCategoryList, gameStructureStates, lockedStates)
                }
            }
            buttonState(isLocked) { text, lock ->
                buttonText = text
                isLocked = lock
            }
        },
            content = {
                ViewText(buttonText)
            }
        )
    }, "Return to Seasons screen") {paddingValues ->
        teamCategoryList = teamCategoryState.value.data?.sortedBy {it.name.trim().uppercase()}!!
        val seasonTeamCategories = state.value.data?.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }
            ?.associateBy { it.teamCategoryId }
        for (teamCategory in teamCategoryList) {
            val seasonTeamCategory = seasonTeamCategories?.get(teamCategory.id)
            gameStructureStates += seasonTeamCategory?.games ?: 0
            lockedStates += seasonTeamCategory?.locked ?: false
        }
        Column(modifier = Modifier.padding(paddingValues), content = {
            teamCategoryDataTable(teamCategoryList, { isLocked }, DataTableConfig.default(isIndexed = false), {
                index -> MatchStructures.entries[gameStructureStates[index].toInt()].display
            }, {
              index, state -> gameStructureStates[index] = state.toShort()
                buttonText = "Save"
            },
                {
                index -> lockedStates[index]
            }, {
                index, state -> lockedStates[index] = state
                    buttonText = "Save"
            })
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
    teamCategoryList.forEachIndexed {  index, item ->
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
}

private fun buttonState(isLocked : Boolean, result : (text : String, lock : Boolean) -> Unit) {
    if (isLocked) {
        result("", false)
    } else {
        result("Edit", true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun teamCategoryDataTable(
    teamCategoryList: List<TeamCategory>,
    isLocked: () -> Boolean,
    config: DataTableConfig,
    getMatch: (index : Int) -> String,
    setMatch: (index : Int, state : Int) -> Unit,
    getLocked: (index : Int) -> Boolean,
    setLocked: (index : Int, state : Boolean) -> Unit
) {
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val table = remember {
        DataTable<TeamCategory>(config = config, scope = scope, lazyState = lazyState)
            .composable(name = "Match Structure", content = { location, _ ->
                Dropdown(isLocked, location.layoutRowIndex - 1, getMatch, setMatch) })
            .composable(name = "Locked", content = { location, _ ->
                Checkbox(
                    checked = getLocked(location.layoutRowIndex - 1),
                    onCheckedChange = {
                        if (!isLocked()) {
                            setLocked(location.layoutRowIndex - 1, it)
                        }}
                ) })
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

@Composable
private fun Dropdown(isLocked: () -> Boolean, index : Int, getMatch: (index : Int) -> String, setMatch: (index : Int, state : Int) -> Unit) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { if (!isLocked()) {expanded.value = !expanded.value} },
    ) {
        ViewText(getMatch(index))
        Icon(
            Icons.Filled.ArrowDropDown, "contentDescription",
            Modifier.align(Alignment.CenterEnd)
        )
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            for (ms in MatchStructures.entries) {
                DropdownMenuItem(
                    onClick = {
                        setMatch(index, ms.ordinal)
                        expanded.value = false
                    }
                ) {
                    Text(text = ms.display)
                }
            }
        }
    }
}