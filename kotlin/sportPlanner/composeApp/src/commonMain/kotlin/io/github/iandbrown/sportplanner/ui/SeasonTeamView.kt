package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlin.collections.set
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

class SeasonTeamViewModel : BaseViewModel<SeasonTeamDao, SeasonTeam>() {
    override fun getDao(db: AppDatabase): SeasonTeamDao = db.getSeasonTeamDao()
}

@Composable
fun NavigateSeasonTeam(navController: NavController, argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonTeamEditor(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
        argument.startsWith("ByCategory&") -> SeasonTeamByCategory(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(11)))
    }
}

@Composable
private fun SeasonTeamEditor(navController: NavController, param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()
    val associationState = koinInject<AssociationViewModel>().uiState.collectAsState()
    val teamCategory = koinInject<TeamCategoryViewModel>().uiState.collectAsState()
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Pair<Short, Short>, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""
    val values = mutableMapOf<Pair<Short, Short>, Short>()

    ViewCommon(
        MergedState(state.value, associationState.value, teamCategory.value),
        navController,
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        {},
        "Return to Seasons screen",
        {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton("By Category", Modifier.weight(1.3f), isLocked) {
                    navController.navigate("${Editors.SEASON_TEAMS.name}/ByCategory&${Json.encodeToString(param)}")
                }
                OutlinedTextButton(value = buttonText,modifier = Modifier.weight(1f)){
                    if (!isLocked && edits.isNotEmpty()) {
                        coroutineScope.launch {
                            for ((key, count) in edits) {
                                if (values.getOrDefault(key, 0) != count) {
                                    viewModel.insert(
                                        SeasonTeam(
                                            seasonId = param.seasonId,
                                            teamCategoryId = key.second,
                                            associationId = key.first,
                                            competitionId = param.competitionId,
                                            count = count
                                        )
                                    )
                                }
                            }
                            edits.clear()
                        }
                    }
                    isLocked = !isLocked
                }
            }
        }
    ) { paddingValues ->
        val teamCategoryList = teamCategory.value.data?.sortedBy { it.name.uppercase().trim() }
        val associationList = associationState.value.data?.sortedBy { it.name.trim().uppercase() }
        for (seasonTeam in state.value.data!!) {
            if (seasonTeam.seasonId == param.seasonId) {
                values[Pair(seasonTeam.associationId, seasonTeam.teamCategoryId)] = seasonTeam.count
            }
        }

        LazyVerticalGrid(columns = DoubleFirstGridCells(teamCategoryList?.size!!),
            modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
            item { ReadonlyViewText("") }
            for (teamCategory in teamCategoryList) {
                item { ReadonlyViewText(teamCategory.name) }
            }
            for (association in associationList!!) {
                item { ReadonlyViewText(association.name) }
                for (team in teamCategoryList) {
                    item {
                        val key = Pair(association.id, team.id)
                        val value = if (edits.contains(key)) edits[key] else values.getOrDefault(key, 0)
                        if (isLocked) {
                            ReadonlyViewText(value?.toString() ?: "")
                        } else {
                            ViewTextField(
                                value = value.toString(),
                                onValueChange = {matchStructure(it) { number -> edits[key] = number } })
                        }
                    }
                }
            }
        }
    }
}

private fun matchStructure(value: String, editFun: (Short) -> Unit) {
    try {
        when(value.toIntOrNull()) {
            0 -> editFun(0)
            1 -> editFun(1)
            2 -> editFun(2)
        }
    } catch (_: NumberFormatException) {
    }
}

@Composable
private fun SeasonTeamByCategory(navController: NavController, param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()
    val teamCategory = koinInject<TeamCategoryViewModel>().uiState.collectAsState()
    val associationState = koinInject<AssociationViewModel>().uiState.collectAsState()
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Short, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""

    ViewCommon(
        MergedState(state.value, teamCategory.value, associationState.value),
        navController,
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        {},
        "Return to Season teams screen",
        {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton(buttonText, Modifier.weight(1f)){
                    if (!isLocked && edits.isNotEmpty()) {
                        coroutineScope.launch {
                            for ((key, count) in edits) {
                                for (association in associationState.value.data!!) {
                                    viewModel.insert(
                                        SeasonTeam(
                                            seasonId = param.seasonId,
                                            teamCategoryId = key,
                                            associationId = association.id,
                                            competitionId = param.competitionId,
                                            count = count
                                        )
                                    )
                                }
                            }
                            edits.clear()
                        }
                    }
                    isLocked = !isLocked
                }
            }
        }
    ) { paddingValues ->
        val teamCategoryList = teamCategory.value.data?.sortedBy { it.name.uppercase().trim() }
        val values = countByTeamCategory(teamCategoryList!!,
            associationState.value.data?.count()!!,
            state.value.data?.
                filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }!!)

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Team Category") }
            item { ReadonlyViewText("Team Count") }
            for (teamCategory in teamCategoryList) {
                item { ReadonlyViewText(teamCategory.name) }
                item {
                    val key = teamCategory.id
                    val value = if (edits.contains(key)) {
                        edits[key]?.toString()!!
                    } else if (values.contains(key) && values[key]!!.toInt() != -1) {
                        values[key]?.toString()!!
                    } else {
                        ""
                    }
                    if (isLocked) {
                        ReadonlyViewText(value)
                    } else {
                        ViewTextField(
                            value = value,
                            onValueChange = {matchStructure(it) { number -> edits[key] = number } })
                    }
                }
            }
        }
    }
}

fun countByTeamCategory(teamCategoryList: List<TeamCategory>, associationCount: Int, seasonTeams: List<SeasonTeam>) : Map<Short, Short> {
    val result = mutableMapOf<Short, Short>()
    val valueCounts = teamCategoryList.associateBy({it.id}, {0}).toMutableMap()
    for (seasonTeam in seasonTeams) {
        valueCounts[seasonTeam.teamCategoryId] = valueCounts.getOrDefault(seasonTeam.teamCategoryId, 0) + 1

        when {
            // doesn't exist so seed
            result[seasonTeam.teamCategoryId] == null -> result[seasonTeam.teamCategoryId] = seasonTeam.count
            // match, ignore
            result[seasonTeam.teamCategoryId] == seasonTeam.count -> {}
            // different, mark...
            else -> result[seasonTeam.teamCategoryId] = -1
        }
    }
    
    for (entry in valueCounts) {
        if (entry.value != associationCount || !result.contains(entry.key)) {
            result[entry.key] = -1
        }
    }

    return result
}
