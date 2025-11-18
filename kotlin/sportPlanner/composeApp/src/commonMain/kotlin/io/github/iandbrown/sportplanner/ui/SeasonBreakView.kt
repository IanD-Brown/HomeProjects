package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonBreakDao
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class SeasonBreakViewModel : BaseViewModel<SeasonBreakDao, SeasonBreak>() {
    override fun getDao(db: AppDatabase): SeasonBreakDao = db.getSeasonBreakDao()
}

private val editor : Editors = Editors.SEASON_BREAK
@Serializable
private data class SeasonBreakEditorInfo(val param : Season, val seasonBreak : SeasonBreak? = null)

@Composable
fun NavigateSeasonBreak(navController : NavController, argument : String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonBreakView(navController, Json.decodeFromString<Season>(argument.substring(5)))
        else -> SeasonBreakEditor(navController, Json.decodeFromString<SeasonBreakEditorInfo>(argument))
    }
}

@Composable
@Preview
private fun SeasonBreakView(navController: NavController, param : Season) {
    val viewModel : SeasonBreakViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(state.value,
        navController,
        "Season breaks in ${param.name}",
        { CreateFloatingAction(navController, editor.editRoute(SeasonBreakEditorInfo(param))) },
        content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(
                items = state.value.data?.sortedBy { it.week }!!,
                key = { seasonBreak -> seasonBreak.id }) { seasonBreak ->
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    Row(
                        modifier = Modifier.weight(2F),
                        content = {
                            SpacedViewText(seasonBreak.name)
                            SpacedViewText(convertMillisToDate(seasonBreak.week))
                        })
                    ItemButtons(
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
private fun SeasonBreakEditor(navController: NavController, info : SeasonBreakEditorInfo) {
    val viewModel : SeasonBreakViewModel = koinInject()
    val seasonCompetitionState = koinInject<SeasonCompetitionViewModel>().uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val title : String
    val name = remember { mutableStateOf("") }
    val week = remember {mutableLongStateOf(0L)}

    when (info.seasonBreak) {
        is SeasonBreak -> {
            week.longValue = info.seasonBreak.week
            title = "Edit Season break"
            name.value = info.seasonBreak.name
        }
        else -> {
            title = "Add Season break"
        }
    }

    ViewCommon(seasonCompetitionState.value, navController, title, {}, "Return to season breaks", bottomBar = {
        Button(
            onClick = {
                coroutineScope.launch {
                    if (info.seasonBreak == null) {
                        viewModel.insert(SeasonBreak(name = name.value.trim(), week = week.longValue))
                    } else {
                        viewModel.update(SeasonBreak(info.seasonBreak.id, name.value.trim(), week.longValue))
                    }
                    navController.popBackStack()
                }
            },
            enabled = !name.value.isEmpty() && week.longValue > 0
        ) { ViewText(stringResource(Res.string.ok)) }
    }) {
        var startDate = 0L
        var endDate = 0L
        for (seasonCompetition in seasonCompetitionState.value.data!!) {
            if (seasonCompetition.seasonId == info.param.id) {
                if (seasonCompetition.startDate > 0 && (startDate == 0L || startDate < seasonCompetition.startDate)) {
                    startDate = seasonCompetition.startDate
                }
                if (seasonCompetition.endDate > 0 && (endDate == 0L || endDate > seasonCompetition.endDate)) {
                    endDate = seasonCompetition.endDate
                }
            }
        }
        PreferableMaterialTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { CreateTopBar(navController, title, "Return to season breaks") },
                content = { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
                        val modifier = Modifier.padding(0.dp)
                        Row {
                            ViewText("Name", modifier = Modifier.width(200.dp).padding(0.dp))
                            ViewTextField(name.value, modifier) { name.value = it }
                        }
                        Row {
                            ViewText("Week", modifier = Modifier.width(200.dp).padding(0.dp))
                            DatePickerView(
                                week.longValue,
                                modifier,
                                { utcMs -> isMondayIn(startDate, endDate, utcMs) }) {
                                week.longValue = it
                            }
                        }
                    }
                })
        }
    }
}