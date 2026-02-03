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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class SeasonTeamViewModel(seasonId : SeasonId, competitionId : CompetitionId) :
    BaseSeasonCompCRUDViewModel<SeasonTeamDao, SeasonTeam>(
        seasonId,
        competitionId,
        inject<SeasonTeamDao>(SeasonTeamDao::class.java).value
    )

@Composable
fun NavigateSeasonTeam(argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonTeamEditor(Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
        argument.startsWith("ByCategory&") -> SeasonTeamByCategory(Json.decodeFromString<SeasonCompetitionParam>(argument.substring(11)))
    }
}

@Composable
private fun SeasonTeamEditor(param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject { parametersOf(param.seasonId, param.competitionId) }
    val state = viewModel.uiState.collectAsState()
    val associationState = koinInject<AssociationViewModel>().uiState.collectAsState(emptyList())
    val teamCategory = koinInject<TeamCategoryViewModel>().uiState.collectAsState(emptyList())
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Pair<Short, Short>, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""
    val values = mutableMapOf<Pair<Short, Short>, Short>()

    ViewCommon(
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        description = "Return to Seasons screen",
        bottomBar = {
            Row {
                ReadonlyViewText("", Modifier.weight(4f))
                OutlinedTextButton("By Category", Modifier.weight(1.3f), isLocked) {
                    appNavController.navigate("${Editors.SEASON_TEAMS.name}/ByCategory&${Json.encodeToString(param)}")
                }
                OutlinedTextButton(value = buttonText,modifier = Modifier.weight(1f)){
                    if (!isLocked && edits.isNotEmpty()) {
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
                    isLocked = !isLocked
                }
            }
        },
        content = { paddingValues ->
            val teamCategoryList = teamCategory.value.sortedBy { it.name.uppercase().trim() }
            val associationList = associationState.value.sortedBy { it.name.trim().uppercase() }
            for (seasonTeam in state.value.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }) {
                values[Pair(seasonTeam.associationId, seasonTeam.teamCategoryId)] = seasonTeam.count
            }

            LazyVerticalGrid(columns = DoubleFirstGridCells(teamCategoryList.size),
                modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
                item { ReadonlyViewText("") }
                for (teamCategory in teamCategoryList) {
                    item { ReadonlyViewText(teamCategory.name) }
                }
                for (association in associationList) {
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
    )
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
private fun SeasonTeamByCategory(param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject { parametersOf(param.seasonId, param.competitionId)}
    val state = viewModel.uiState.collectAsState()
    val teamCategory = koinInject<TeamCategoryViewModel>().uiState.collectAsState(emptyList())
    val associationState = koinInject<AssociationViewModel>().uiState.collectAsState(emptyList())
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Short, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""

    ViewCommon(
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        description = "Return to Season teams screen",
        bottomBar = {
            BottomBarWithButton(buttonText) {
                if (!isLocked && edits.isNotEmpty()) {
                    for ((key, count) in edits) {
                        for (association in associationState.value) {
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
                isLocked = !isLocked
            }
        },
        content = { paddingValues ->
            val teamCategoryList = teamCategory.value.sortedBy { it.name.uppercase().trim() }
            val values = countByTeamCategory(teamCategoryList,
                associationState.value.count(),
                state.value.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId })

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
    )
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
