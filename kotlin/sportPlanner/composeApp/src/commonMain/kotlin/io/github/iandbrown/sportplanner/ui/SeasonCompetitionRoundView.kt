package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.logic.DayDate
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class SeasonCompetitionRoundViewModel : BaseViewModel<SeasonCompetitionRoundDao, SeasonCompetitionRound>() {
    override fun getDao(db: AppDatabase): SeasonCompetitionRoundDao = db.getSeasonCompetitionRoundDao()
}

private val editor : Editors = Editors.SEASON_COMPETITION_ROUND
@Serializable
private data class SeasonCompetitionRoundEditorInfo(val param : SeasonCompetitionParam, val competitionRound : SeasonCompetitionRound? = null)

@Composable
fun NavigateSeasonCompetitionRound(argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonCompetitionView(Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
        else -> SeasonCompetitionRoundEditor(Json.decodeFromString<SeasonCompetitionRoundEditorInfo>(argument))
    }
}

@Composable
private fun SeasonCompetitionView(param: SeasonCompetitionParam) {
    val viewModel : SeasonCompetitionRoundViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        state.value,
        "Competition rounds in ${param.seasonName} for ${param.competitionName}",
        bottomBar = { BottomBarWithButtonN("+") { editor.editRoute(SeasonCompetitionRoundEditorInfo(param)) } },
        content = { paddingValues ->
            val values = state.value.data?.filter { it.seasonId == param.seasonId && it.competitionId == param.competitionId }?.sortedBy { it.round }!!
            LazyVerticalGrid(columns = WeightedIconGridCells(2, 1, 2, 2, 2), Modifier.padding(paddingValues)) {
                item { ViewText("Round") }
                item { ViewText("Description") }
                item { ViewText("Week") }
                item { ViewText("Optional") }
                item { Icon(Blank, "") }
                item { Icon(Blank, "") }
                for (it in values) {
                    item {ViewText(it.round.toString())}
                    item {ViewText(it.description)}
                    item {ViewText(DayDate(it.week).toString())}
                    item {Checkbox(checked = it.optional, onCheckedChange = null, enabled = false)}
                    item { EditButton { editor.editRoute(SeasonCompetitionRoundEditorInfo(param, it)) }}
                    item { DeleteButton { coroutineScope.launch { viewModel.delete(it) } }}
                }
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonCompetitionRoundEditor(info: SeasonCompetitionRoundEditorInfo) {
    val seasonParameter = parametersOf(info.param.seasonId)
    val viewModel : SeasonCompetitionRoundViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val seasonCompetitionState = koinInject<SeasonCompetitionViewModel>{seasonParameter}.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val description = remember { mutableStateOf("") }
    val week = remember { mutableIntStateOf(0) }
    val optional = remember {mutableStateOf( false )}
    val validRound = remember { mutableStateOf(true) }
    val title = if (info.competitionRound == null) "Add Competition round" else "Edit Competition round"
    val round = remember {mutableStateOf<Short>(0)}

    ViewCommon(
        MergedState(state.value, seasonCompetitionState.value),
        title,
        bottomBar = {
            BottomBarWithButton(enabled = !description.value.isEmpty() && validRound.value) {
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
                    appNavController.popBackStack()
                }
            }
        }) { paddingValues ->
        val rounds = getRounds(state.value.data!!, info)
        val season = seasonCompetitionState.value.data
            ?.first { it.seasonId == info.param.seasonId && it.competitionId == info.param.competitionId }!!

        when (info.competitionRound) {
            is SeasonCompetitionRound -> {
                round.value = info.competitionRound.round
                description.value = info.competitionRound.description
                week.intValue = info.competitionRound.week
                optional.value = info.competitionRound.optional
            }
            else -> {
                if (rounds.isEmpty()) {
                    round.value = 1.toShort()
                } else {
                    round.value = (rounds.max() + 1).toShort()
                }
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(4), Modifier.padding(paddingValues)) {
            item { ReadonlyViewText("Round") }
            item { ReadonlyViewText("Description") }
            item { ReadonlyViewText("Week") }
            item { ReadonlyViewText("Optional") }
            item {
                ViewTextField(
                    value = round.value.toString(),
                    modifier = Modifier,
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
            item { ViewTextField(description.value) { description.value = it } }
            item {
                DatePickerView(week.intValue, Modifier, { utcMs -> isMondayIn(season, utcMs) }) {
                    week.intValue = it
                }
            }
            item { Checkbox(checked = optional.value, onCheckedChange = { optional.value = it }) }
        }
    }
}


private fun getRounds(data: List<SeasonCompetitionRound>, info: SeasonCompetitionRoundEditorInfo): Set<Short> {
    return data.filter { it.seasonId == info.param.seasonId && it.competitionId == info.param.competitionId }
        .map { it.round }
        .toSet()
}
