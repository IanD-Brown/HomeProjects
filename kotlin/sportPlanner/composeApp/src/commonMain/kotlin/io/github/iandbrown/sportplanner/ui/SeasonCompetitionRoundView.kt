package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.logic.DayDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class SeasonCompetitionRoundViewModel : BaseViewModel<SeasonCompetitionRoundDao, SeasonCompetitionRound>() {
    override fun getDao(db: AppDatabase): SeasonCompetitionRoundDao = db.getSeasonCompetitionRoundDao()
}

private val editor : Editors = Editors.SEASON_COMPETITION_ROUND
@Serializable
private data class SeasonCompetitionRoundEditorInfo(val param : SeasonCompetitionParam, val competitionRound : SeasonCompetitionRound? = null)

@Composable
fun NavigateSeasonCompetitionRound(navController : NavController, argument : String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonCompetitionView(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
        else -> SeasonCompetitionRoundEditor(navController, Json.decodeFromString<SeasonCompetitionRoundEditorInfo>(argument))
    }
}

@Composable
@Preview
private fun SeasonCompetitionView(navController : NavController, param : SeasonCompetitionParam) {
    val viewModel : SeasonCompetitionRoundViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(state.value,
        navController,
        "Competition rounds in ${param.seasonName} for ${param.competitionName}",
        { CreateFloatingAction(navController, editor.editRoute(SeasonCompetitionRoundEditorInfo(param))) },
        content = { paddingValues ->
            val columns = 11
             Column(Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                val values = state.value.data?.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }?.sortedBy { it.round }!!
                Row {
                    ViewText("Round", modifier = Modifier.weight(1f))
                    SpacedViewText("Description", modifier = Modifier.weight(1f))
                    SpacedViewText("Week", modifier = Modifier.weight(1f))
                    SpacedViewText("Optional", modifier = Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier)
                    Icon(Blank, "")
                    Spacer(Modifier)
                    Icon(Blank, "")
                }
                FlowRow(
                    modifier = Modifier.padding(4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.aligned(Alignment.Start),
                    maxItemsInEachRow = columns
                ) {
                    for (it in values) {
                        ViewText(it.round.toString(), modifier = Modifier.weight(1f))
                        SpacedViewText(it.description, modifier = Modifier.weight(1f))
                        SpacedViewText(DayDate(it.week).toString(), modifier = Modifier.weight(1f))
                        Checkbox(checked = it.optional, onCheckedChange = null, enabled = false, modifier = Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                        ItemButtons(
                            { navController.navigate(editor.editRoute(SeasonCompetitionRoundEditorInfo(param, it))) },
                            { coroutineScope.launch { viewModel.delete(it) } })
                    }
                }
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun SeasonCompetitionRoundEditor(navController: NavController, info : SeasonCompetitionRoundEditorInfo) {
    val viewModel : SeasonCompetitionRoundViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val seasonCompetitionState = koinInject<SeasonCompetitionViewModel>().uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val description = remember { mutableStateOf("") }
    val week = remember { mutableIntStateOf(0) }
    val optional = remember {mutableStateOf( false )}
    val validRound = remember { mutableStateOf(true) }
    var title : String

     if (state.value.isLoading || seasonCompetitionState.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else if (state.value.data != null && seasonCompetitionState.value.data != null) {
        val rounds = getRounds(state.value.data!!, info)
        val season = seasonCompetitionState.value.data
            ?.first { it.seasonId == info.param.seasonId && it.competitionId == info.param.competitionId }!!
        val round = remember {mutableStateOf<Short>(0)}

        when (info.competitionRound) {
            is SeasonCompetitionRound -> {
                round.value = info.competitionRound.round
                description.value = info.competitionRound.description
                week.intValue = info.competitionRound.week
                optional.value = info.competitionRound.optional
                title = "Edit Competition round"
            }
            else -> {
                if (rounds.isEmpty()) {
                    round.value = 1.toShort()
                } else {
                    round.value = (rounds.max() + 1).toShort()
                }
                title = "Add Season Competition round"
            }
        }
        PreferableMaterialTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { CreateTopBar(navController, title, "Return to season competition rounds") },
                content = { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
                        val modifier = Modifier.padding(0.dp)
                        Row {
                            ViewText("Round", Modifier.width(200.dp))
                            ViewTextField(
                                value = round.value.toString(),
                                modifier = modifier,
                                isError = { !validRound.value }) {
                                try {
                                    round.value = it.toShort()
                                    validRound.value = round.value > 0 &&
                                            (info.competitionRound != null && info.competitionRound.round == round.value
                                                    || !rounds.contains(round.value))
                                } catch (_: NumberFormatException) {
                                    validRound.value = false
                                }
                            }
                        }
                        Row {
                            ViewText("Description", modifier = Modifier.width(200.dp))
                            ViewTextField(description.value, modifier) { description.value = it }
                        }
                        Row {
                            ViewText("Week", modifier = Modifier.width(200.dp))
                            DatePickerView(week.intValue, modifier, { utcMs -> isMondayIn(season, utcMs) }) {
                                week.intValue = it
                            }
                        }
                        Row {
                            ViewText("Optional", modifier = Modifier.width(200.dp))
                            Checkbox(checked = optional.value, onCheckedChange = { optional.value = it })
                        }
                    }
                }, bottomBar = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                when (info.competitionRound) {
                                    is SeasonCompetitionRound -> {
                                        viewModel.update(
                                            SeasonCompetitionRound(
                                                info.competitionRound.seasonId,
                                                info.competitionRound.competitionId,
                                                round.value,
                                                description.value.trim(),
                                                week.intValue,
                                                optional.value
                                            )
                                        )
                                    }
                                    else -> {
                                        viewModel.insert(
                                            SeasonCompetitionRound(
                                                info.param.seasonId,
                                                info.param.competitionId,
                                                round.value,
                                                description.value.trim(),
                                                week.intValue,
                                                optional.value
                                            )
                                        )
                                    }
                                }
                                navController.popBackStack()
                            }
                        },
                        enabled = !description.value.isEmpty() && validRound.value
                    ) { ViewText(stringResource(Res.string.ok)) }
                })
        }
    }
}

private fun getRounds(data: List<SeasonCompetitionRound>, info: SeasonCompetitionRoundEditorInfo): Set<Short> {
    return data.filter { it.seasonId == info.param.seasonId && it.competitionId == info.param.competitionId }
        .map { it.round }
        .toSet()
}
