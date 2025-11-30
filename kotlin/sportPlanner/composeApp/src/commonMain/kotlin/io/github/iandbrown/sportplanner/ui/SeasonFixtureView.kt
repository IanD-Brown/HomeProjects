package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonFixtureViewDao
import io.github.iandbrown.sportplanner.logic.SeasonWeeks.Companion.createSeasonWeeks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class SeasonFixtureViewModel : ViewModel {
    val database : AppDatabase by inject(AppDatabase::class.java)
    val dao : SeasonFixtureViewDao = database.getSeasonFixtureViewDao()
    private val _uiState = MutableStateFlow(UiState<SeasonFixtureView>(true))
    val uiState = _uiState.asStateFlow()

    constructor(seasonId : Short) {
        viewModelScope.launch {
            flow {
                emit(dao.get(seasonId))
            }.collect {
                _uiState.value = UiState(data = it, isLoading = false)
            }
        }
    }
}

private val editor = Editors.SEASON_FIXTURES

@Composable
fun NavigateFixtures(navController: NavController, argument : String?) =
    when (argument) {
        "View" -> FixtureView(navController)
        else -> FixtureTableVIew(navController, Json.decodeFromString<Season>(argument!!))
    }

@Composable
@Preview
private fun FixtureView(navController: NavController) {
    val seasonState by koinInject<SeasonViewModel>().uiState.collectAsState()
    val calculating = remember {mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()

    if (calculating.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        ViewCommon(seasonState, navController, "Fixtures", {})
        { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(
                    items = seasonState.data?.sortedByDescending { it.name.trim().uppercase() }!!,
                    key = { it.id }) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            ViewText(it.name)
                        }
                        SpacedIcon(Icons.Filled.GridView, "Show fixtures") {
                            navController.navigate(editor.editRoute(it))
                        }
                        SpacedIcon(Icons.Filled.Calculate, "Calculate fixtures") {
                            calculating.value = true
                            coroutineScope.launch {
                                calcFixtures(it.id)
                            }
                            calculating.value = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixtureTableVIew(navController : NavController, season : Season) {
    val viewModel : SeasonFixtureViewModel = koinViewModel { parametersOf(season.id) }
    val state = viewModel.uiState.collectAsState()

    ViewCommon(state.value,
        navController,
        "Season fixtures",
        { },
        " Return to seasons screen",
        content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            val values = state.value.data!!
            items(
                items = values.sortedBy { it.date },
                key = { seasonFixture -> seasonFixture.id }) { seasonFixture ->
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    SpacedViewText(convertMillisToDate(seasonFixture.date))
                    SpacedViewText(seasonFixture.teamCategoryName)
                    SpacedViewText(seasonFixture.message)
                    SpacedViewText(teamName(seasonFixture.homeAssociation, seasonFixture.homeTeamNumber))
                    SpacedViewText(teamName(seasonFixture.awayAssociation, seasonFixture.awayTeamNumber))
                })
            }
        })
    })
}

private fun teamName(association : String, number : Short) : String {
    val postfix = when (number) {
        0.toShort() -> ""
        1.toShort() -> " A"
        else -> " B"
    }
    return "$association$postfix"
}

private suspend fun calcFixtures(seasonId : Short) {
    val db : AppDatabase by inject(AppDatabase::class.java)
    val calculateTeamCategories = db.getSeasonTeamCategoryDao().getBySeason(seasonId).filter {!it.locked}
    for (teamCategory in calculateTeamCategories) {
        db.getSeasonFixtureDao().deleteBySeasonTeamCategory(seasonId, teamCategory.teamCategoryId)
    }
    val seasonWeeks = createSeasonWeeks(seasonId)

    for (seasonBreak in seasonWeeks.breakWeeks()) {
            for (teamCategory in calculateTeamCategories) {
                db.getSeasonFixtureDao().insert(SeasonFixture(0,
                    seasonId,
                    teamCategory.teamCategoryId,
                    seasonBreak.key,
                    seasonBreak.value,
                    0.toShort(),
                    0.toShort(),
                    0.toShort(),
                    0.toShort()))
        }
    }
}
