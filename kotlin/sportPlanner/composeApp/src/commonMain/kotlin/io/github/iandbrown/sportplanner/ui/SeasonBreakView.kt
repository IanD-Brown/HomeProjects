package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonBreakDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.logic.DayDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class SeasonBreakViewModel(seasonId : Short) : BaseSeasonViewModel<SeasonBreakDao, SeasonBreak>(seasonId) {
    override fun getDao(db: AppDatabase): SeasonBreakDao = db.getSeasonBreakDao()
}

private val editor : Editors = Editors.SEASON_BREAK
@Serializable
private data class SeasonBreakEditorInfo(val param : Season, val seasonBreak : SeasonBreak? = null)

@Composable
fun NavigateSeasonBreak(argument: String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> SeasonBreakView(Json.decodeFromString<Season>(argument.substring(5)))
        else -> SeasonBreakEditor(Json.decodeFromString<SeasonBreakEditorInfo>(argument))
    }
}

@Composable
private fun SeasonBreakView(param: Season) {
    val viewModel : SeasonBreakViewModel = koinViewModel { parametersOf(param.id) }
    val state = viewModel.uiState.collectAsState()

    ViewCommon(
        state.value,
        "Season breaks in ${param.name}",
        {  },
        bottomBar = { BottomBarWithButtonN("+") { editor.editRoute(SeasonBreakEditorInfo(param)) }},
        content = { paddingValues ->
            LazyVerticalGrid(columns = TrailingIconGridCells(2, 2),
                modifier = Modifier.padding(paddingValues)) {
                item { ViewText("Name") }
                item { ViewText("Week") }
                item {}
                item {}
                for (seasonBreak in state.value.data?.sortedBy { it.week }!!) {
                    item { ViewText(seasonBreak.name) }
                    item { ViewText(DayDate(seasonBreak.week).toString()) }
                    item { EditButton { editor.editRoute(SeasonBreakEditorInfo(param, seasonBreak)) } }
                    item { DeleteButton { viewModel.delete(seasonBreak) } }
                }
            }
        })
}

@Composable
private fun SeasonBreakEditor(info: SeasonBreakEditorInfo) {
    val seasonParameter = parametersOf(info.param.id)
    val viewModel : SeasonBreakViewModel = koinViewModel { seasonParameter }
    val seasonCompetitionState = koinInject<SeasonCompetitionViewModel> { seasonParameter }.uiState.collectAsState()
    val title = if (info.seasonBreak == null) "Add Season break" else "Edit Season break"
    var name by remember { mutableStateOf(info.seasonBreak?.name ?: "") }
    var week by remember {mutableIntStateOf(info.seasonBreak?.week ?: 0)}

    ViewCommon(
        seasonCompetitionState.value,
        title,
        description = "Return to season breaks",
        bottomBar = {
            BottomBarWithButton {
                save(info, viewModel, name, week)
                appNavController.popBackStack()
            }
        },
        confirm = { name.isNotEmpty() && (info.seasonBreak == null || name != info.seasonBreak.name) ||
                (info.seasonBreak != null && week != info.seasonBreak.week) },
        confirmAction = {save(info, viewModel, name, week)},
        content = { paddingValues ->
            val range = buildDateRange(seasonCompetitionState.value.data!!)
            LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
                item { ReadonlyViewText("Name") }
                item { ReadonlyViewText("Week") }
                item { ViewTextField(name) { name = it } }
                item { DatePickerView(
                    week,
                    Modifier.padding(0.dp),
                    { utcMs -> DayDate.isMondayIn(range, DayDate(utcMs).value()) }) {
                    week = it
                } }
            }
        })
}

private fun save(info: SeasonBreakEditorInfo, viewModel: SeasonBreakViewModel, name: String, week: Int) {
    if (info.seasonBreak == null) {
        viewModel.insert(SeasonBreak(seasonId = info.param.id, name = name.trim(), week = week))
    } else {
        viewModel.update(SeasonBreak(info.seasonBreak.id, info.param.id, name.trim(), week))
    }
}

private fun buildDateRange(seasonCompetitions: List<SeasonCompetition>): IntRange {
    var startDate = 0
    var endDate = 0
    for (seasonCompetition in seasonCompetitions) {
        if (seasonCompetition.startDate > 0 && (startDate == 0 || startDate > seasonCompetition.startDate)) {
            startDate = seasonCompetition.startDate
        }
        if (seasonCompetition.endDate > 0 && (endDate == 0 || endDate < seasonCompetition.endDate)) {
            endDate = seasonCompetition.endDate
        }
    }
    return IntRange(startDate, endDate)
}
