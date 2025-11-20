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
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.logic.SeasonWeeks
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject

private val editor = Editors.SEASON_FIXTURES

@Composable
fun NavigateFixtures(navController: NavController, argument : String?) =
    when (argument) {
        "View" -> FixtureView(navController)
        else -> {}
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
                    items = seasonState.data?.sortedBy { it.name.trim().uppercase() }!!,
                    key = { it.id }) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            ViewText(it.name)
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

private suspend fun calcFixtures(seasonId : Short) {
    val db : AppDatabase by inject(AppDatabase::class.java)
    val seasonWeeks = SeasonWeeks(db.getSeasonCompetitionDao().getBySeason(seasonId), db.getSeasonBreakDao().getBySeason(seasonId)
    )
}
