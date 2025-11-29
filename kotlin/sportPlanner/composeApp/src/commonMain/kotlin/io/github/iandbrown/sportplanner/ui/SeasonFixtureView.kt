package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureDao
import io.github.iandbrown.sportplanner.logic.SeasonWeeks.Companion.createSeasonWeeks
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

class SeasonFixtureViewModel : BaseViewModel<SeasonFixtureDao, SeasonFixture> {
    override fun getDao(db: AppDatabase): SeasonFixtureDao = db.getSeasonFixtureDao()

    constructor(seasonId : Short) : super(false) {
        _uiState.value = UiState<SeasonFixture>(true)
        viewModelScope.launch {
            flow {
                val value = dao.getBySeason(seasonId)
                println("$seasonId XXXX $value")
                emit(value)
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
                    SpacedViewText(seasonFixture.teamCategoryId.toString())
                    SpacedViewText(seasonFixture.message)
                })
            }
        })
    })
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
