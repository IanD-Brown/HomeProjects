package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class SeasonTeamViewModel(
    val seasonId: SeasonId,
    val competitionId: CompetitionId,
    dao: SeasonTeamDao
) : BaseCRUDViewModel<SeasonTeamDao, SeasonTeam>(dao, { it.get(seasonId, competitionId) }) {

    fun saveTeams(
        param: SeasonCompetitionParam,
        edits: Map<Pair<Short, Short>, Short>,
        currentValues: Map<Pair<Short, Short>, Short>
    ) {
        for ((key, count) in edits) {
            if (currentValues.getOrDefault(key, 0) != count) {
                insert(
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
    }

    fun saveByCategory(param: SeasonCompetitionParam, edits: Map<Short, Short>, associations: List<Association>) {
        for ((key, count) in edits) {
            for (association in associations) {
                insert(
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
    }
}

@Composable
fun SeasonTeamScreen(param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val associationViewModel: AssociationViewModel = koinViewModel()
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()

    val state by viewModel.getState().collectAsStateWithLifecycle()
    val associationState by associationViewModel.getState().collectAsStateWithLifecycle()
    val teamCategoryState by teamCategoryViewModel.getState().collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    SeasonTeamContent(
        param = param,
        state = state,
        associationState = associationState,
        teamCategoryState = teamCategoryState,
        onSave = { edits, values ->
            viewModel.saveTeams(param, edits, values)
        },
        onNavigateByCategory = { appNavigator.navigate(Route.SeasonTeamsByCategory(param)) }
    )
}

@Composable
private fun SeasonTeamContent(
    param: SeasonCompetitionParam,
    state: ViewModelState<SeasonTeam>,
    associationState: ViewModelState<Association>,
    teamCategoryState: ViewModelState<TeamCategory>,
    onSave: (Map<Pair<Short, Short>, Short>, Map<Pair<Short, Short>, Short>) -> Unit,
    onNavigateByCategory: () -> Unit
) {
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
                    onNavigateByCategory()
                }
                OutlinedTextButton(value = buttonText, modifier = Modifier.weight(1f)) {
                    if (!isLocked && edits.isNotEmpty()) {
                        onSave(edits, values)
                        edits.clear()
                    }
                    isLocked = !isLocked
                }
            }
        },
        states = persistentListOf(state, associationState, teamCategoryState)
    ) { paddingValues ->
        val teamCategoryList = teamCategoryState.values().sortedBy { it.name.uppercase().trim() }
        val associationList = associationState.values().sortedBy { it.name.trim().uppercase() }
        for (seasonTeam in state.values().filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }) {
            values[Pair(seasonTeam.associationId, seasonTeam.teamCategoryId)] = seasonTeam.count
        }

        LazyVerticalGrid(
            columns = DoubleFirstGridCells(teamCategoryList.size + 1),
            modifier = Modifier.padding(paddingValues).fillMaxWidth()
        ) {
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
                                onValueChange = { matchStructure(it) { number -> edits[key] = number } })
                        }
                    }
                }
            }
        }
    }
}

private fun matchStructure(value: String, editFun: (Short) -> Unit) {
    try {
        val num = value.toShortOrNull()
        if (num != null && num in 0..2) {
            editFun(num)
        }
    } catch (_: NumberFormatException) {
    }
}

@Composable
fun SeasonTeamByCategoryScreen(param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinViewModel { parametersOf(param.seasonId, param.competitionId) }
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()
    val associationViewModel: AssociationViewModel = koinViewModel()

    val state by viewModel.getState().collectAsStateWithLifecycle()
    val teamCategoryState by teamCategoryViewModel.getState().collectAsStateWithLifecycle()
    val associationState by associationViewModel.getState().collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    SeasonTeamByCategoryContent(
        param = param,
        state = state,
        teamCategoryState = teamCategoryState,
        associationState = associationState,
        onSave = { edits ->
            viewModel.saveByCategory(param, edits, associationState.values())
        }
    )
}

@Composable
private fun SeasonTeamByCategoryContent(
    param: SeasonCompetitionParam,
    state: ViewModelState<SeasonTeam>,
    teamCategoryState: ViewModelState<TeamCategory>,
    associationState: ViewModelState<Association>,
    onSave: (Map<Short, Short>) -> Unit
) {
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Short, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""

    ViewCommon(
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        description = "Return to Season teams screen",
        bottomBar = {
            BottomBarWithButton(buttonText) {
                if (!isLocked && edits.isNotEmpty()) {
                    onSave(edits)
                    edits.clear()
                }
                isLocked = !isLocked
            }
        },
        states = persistentListOf(state, teamCategoryState, associationState)
    ) { paddingValues ->
        val teamCategoryList = teamCategoryState.values().sortedBy { it.name.uppercase().trim() }
        val associationsCount = associationState.values().count()
        val values = countByTeamCategory(
            teamCategoryList,
            associationsCount,
            state.values().filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId })

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Team Category") }
            item { ReadonlyViewText("Team Count") }
            for (teamCategory in teamCategoryList) {
                item { ReadonlyViewText(teamCategory.name) }
                item {
                    val key = teamCategory.id
                    val value = if (edits.contains(key)) {
                        edits[key]?.toString() ?: ""
                    } else if (values.contains(key) && values[key] != (-1).toShort()) {
                        values[key]?.toString() ?: ""
                    } else {
                        ""
                    }
                    if (isLocked) {
                        ReadonlyViewText(value)
                    } else {
                        ViewTextField(
                            value = value,
                            onValueChange = { matchStructure(it) { number -> edits[key] = number } })
                    }
                }
            }
        }
    }
}

fun countByTeamCategory(
    teamCategoryList: List<TeamCategory>,
    associationCount: Int,
    seasonTeams: List<SeasonTeam>
): Map<Short, Short> {
    val result = mutableMapOf<Short, Short>()
    val valueCounts = teamCategoryList.associateBy({ it.id }, { 0 }).toMutableMap()
    for (seasonTeam in seasonTeams) {
        valueCounts[seasonTeam.teamCategoryId] = valueCounts.getOrDefault(seasonTeam.teamCategoryId, 0) + 1

        when {
            result[seasonTeam.teamCategoryId] == null -> result[seasonTeam.teamCategoryId] = seasonTeam.count
            result[seasonTeam.teamCategoryId] == seasonTeam.count -> {}
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
