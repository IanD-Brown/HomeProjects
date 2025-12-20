package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
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
import kotlin.getValue
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject

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
                Button(enabled = isLocked, modifier = Modifier.weight(1f), onClick = {
                    navController.navigate(
                        "${Editors.SEASON_TEAMS.name}/ByCategory&${
                            Json.encodeToString(
                                param
                            )
                        }"
                    )
                }) { ViewText("By Category") }
                Button(
                    onClick = {
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
                    },
                    modifier = Modifier.weight(1f)) {
                    ViewText(buttonText)
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
                                onValueChange = {
                                    try {
                                        when(it.toIntOrNull()) {
                                            0 -> edits[key] = 0
                                            1 -> edits[key] = 1
                                            2 -> edits[key] = 2
                                        }
                                    } catch (_: NumberFormatException) {
                                    }
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonTeamByCategory(navController: NavController, param: SeasonCompetitionParam) {
    val viewModel: SeasonTeamViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()
    val teamCategory = koinInject<TeamCategoryViewModel>().uiState.collectAsState()
    var isLocked by remember { mutableStateOf(true) }
    val edits = remember { mutableStateMapOf<Short, Short>() }
    val buttonText = if (isLocked) "Edit" else if (edits.isNotEmpty()) "Save" else ""
    val values = mutableMapOf<Short, Short>()

    ViewCommon(
        MergedState(state.value, teamCategory.value),
        navController,
        "Season ${param.seasonName} Competition ${param.competitionName} Teams",
        {
            FloatingActionButton(
                onClick = {
                    if (!isLocked && edits.isNotEmpty()) {
                        coroutineScope.launch {
                            val db : AppDatabase by inject(AppDatabase::class.java)
                            val associations = db.getAssociationDao().getAll()
                            for ((key, count) in edits) {
                                for (association in associations) {
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
                },
                content = {
                    ViewText(buttonText)
                }
            )
        },
        "Return to Season teams screen"
    ) { paddingValues ->
        val teamCategoryList = teamCategory.value.data?.sortedBy { it.name.uppercase().trim() }
        for (seasonTeam in state.value.data!!) {
            if (seasonTeam.seasonId == param.seasonId) {
                when {
                    // doesn't exist so seed
                    values[seasonTeam.teamCategoryId] == null -> values[seasonTeam.teamCategoryId] = seasonTeam.count
                    // match, ignore
                    values[seasonTeam.teamCategoryId] == seasonTeam.count -> {}
                    // different, mark...
                    else -> values[seasonTeam.teamCategoryId] = -1
                }
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Team Category") }
            item { ReadonlyViewText("Team Count") }
            for (teamCategory in teamCategoryList!!) {
                item { ViewText(teamCategory.name) }
                item {
                    val key = teamCategory.id
                    val value = if (edits.contains(key)) {
                        edits[key]?.toString()!!
                    } else if (values.contains(key)){
                        values[key]?.toString()!!
                    } else {
                        ""
                    }
                    if (isLocked) {
                        ReadonlyViewText(value)
                    } else {
                        ViewTextField(
                            value = value,
                            onValueChange = {
                                try {
                                    when(it.toIntOrNull()) {
                                        null -> edits[key] = 0
                                        0 -> edits[key] = 0
                                        1 -> edits[key] = 1
                                        2 -> edits[key] = 2
                                    }
                                } catch (_: NumberFormatException) {
                                }
                            })
                    }
                }
            }
        }
    }
}
