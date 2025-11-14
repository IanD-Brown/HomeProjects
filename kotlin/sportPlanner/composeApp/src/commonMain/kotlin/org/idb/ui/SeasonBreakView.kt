package org.idb.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.idb.database.AppDatabase
import org.idb.database.SeasonBreak
import org.idb.database.SeasonBreakDao
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

class SeasonBreakViewModel : BaseViewModel<SeasonBreakDao, SeasonBreak>() {
    override fun getDao(db: AppDatabase): SeasonBreakDao = db.getSeasonBreakDao()
}

private val editor : Editors = Editors.SEASON_BREAK
@Serializable
private data class SeasonBreakEditorInfo(val param : SeasonCompetitionParam, val seasonBreak : SeasonBreak? = null)

@Composable
fun navigateSeasonBreak(navController : NavController, argument : String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> seasonBreakView(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
        else -> seasonBreakEditor(navController, Json.decodeFromString<SeasonBreakEditorInfo>(argument))
    }
}

@Composable
@Preview
private fun seasonBreakView(navController: NavController, param : SeasonCompetitionParam) {
    val viewModel : SeasonBreakViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    viewCommon(state.value,
        navController,
        "Season breaks in ${param.seasonName}",
        { createFloatingAction(navController, editor.editRoute(SeasonBreakEditorInfo(param))) },
        content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(
                items = state.value.data?.sortedBy { it.week }!!,
                key = { seasonBreak -> seasonBreak.id }) { seasonBreak ->
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    Row(
                        modifier = Modifier.weight(2F),
                        content = {
                            spacedViewText(seasonBreak.name)
                            spacedViewText(convertMillisToDate(seasonBreak.week))
                        })
                    itemButtons(
                        { navController.navigate(editor.editRoute(SeasonBreakEditorInfo(param, seasonBreak))) },
                        { coroutineScope.launch { viewModel.delete(seasonBreak) } })
                })
            }
        })
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun seasonBreakEditor(navController: NavController, info : SeasonBreakEditorInfo) {
    val viewModel : SeasonBreakViewModel = koinInject()
    val seasonCompetitionState = koinInject<SeasonCompetitionViewModel>().uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val title : String
    val name = remember { mutableStateOf("") }
    val week = remember {mutableStateOf(0L)}

    when (info.seasonBreak) {
        is SeasonBreak -> {
            week.value = info.seasonBreak.week
            title = "Edit Season break"
            name.value = info.seasonBreak.name
        }
        else -> {
            title = "Add Season break"
        }
    }

    if (seasonCompetitionState.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else if (seasonCompetitionState.value.data != null) {
        val season = seasonCompetitionState.value.data
            ?.first { it.seasonId == info.param.seasonId && it.competitionId == info.param.competitionId }!!

        PreferableMaterialTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { createTopBar(navController, title, "Return to season breaks") },
                content = { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
                        val modifier = Modifier.padding(0.dp)
                        Row {
                            ViewText("Name", modifier = Modifier.width(200.dp).padding(0.dp))
                            ViewTextField(name.value, modifier) { name.value = it }
                        }
                        Row {
                            ViewText("Week", modifier = Modifier.width(200.dp).padding(0.dp))
                            datePicker(week.value, modifier, { utcMs -> isMondayIn(season, utcMs) }) {
                                week.value = it
                            }
                        }
                    }
                }, bottomBar = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (info.seasonBreak == null) {
                                    viewModel.insert(SeasonBreak(name = name.value.trim(), week = week.value))
                                } else {
                                    viewModel.update(SeasonBreak(info.seasonBreak.id, name.value.trim(), week.value))
                                }
                                navController.popBackStack()
                            }
                        },
                        enabled = !name.value.isEmpty() && week.value > 0
                    ) { ViewText(stringResource(Res.string.ok)) }
                })
        }
    }
}