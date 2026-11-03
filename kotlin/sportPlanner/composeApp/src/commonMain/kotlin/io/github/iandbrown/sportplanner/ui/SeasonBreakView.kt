package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonBreakDao
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.logic.DayDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class SeasonBreakViewModel(seasonId: SeasonId, dao: SeasonBreakDao) :
    BaseCRUDViewModel<SeasonBreakDao, SeasonBreak>(dao, { it.get(seasonId) }) {

    fun save(seasonId: SeasonId, seasonBreak: SeasonBreak?, name: String, week: Int) {
        if (seasonBreak == null) {
            insert(SeasonBreak(seasonId = seasonId, name = name.trim(), week = week))
        } else {
            update(SeasonBreak(seasonBreak.id, seasonId, name.trim(), week = week))
        }
    }
}

@Composable
fun SeasonBreakListScreen(param: Season) {
    val viewModel: SeasonBreakViewModel = koinViewModel { parametersOf(param.id) }
    val state by viewModel.getState().collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    ViewCommon(
        "Season breaks in ${param.name}",
        bottomBar = {
            BottomBarWithButton("+") {
                appNavigator.navigate(Route.SeasonBreakEdit(param, null))
            }
        },
        states = persistentListOf(state)
    ) { paddingValues ->
        val seasonBreaks = state.values().sortedBy { it.week }.toImmutableList()
        LazyVerticalGrid(
            columns = TrailingIconGridCells(2, 2),
            modifier = Modifier.padding(paddingValues),
        ) {
            viewTextItems(listOf("Name", "Week"))
            item(span = { GridItemSpan(2) }) {}
            for (seasonBreak in seasonBreaks) {
                viewTextItems(listOf(seasonBreak.name, DayDate(seasonBreak.week).toString()))
                editButton {
                    appNavigator.navigate(Route.SeasonBreakEdit(param, seasonBreak))
                }
                deleteButton { viewModel.delete(seasonBreak) }
            }
        }
    }
}

@Composable
fun SeasonBreakEditScreen(
    season: Season,
    seasonBreak: SeasonBreak? = null
) {
    val viewModel: SeasonCompViewModel = koinViewModel()
    val seasonBreakViewModel: SeasonBreakViewModel = koinViewModel { parametersOf(season.id) }
    val seasonCompetitionState by viewModel.getState().collectAsStateWithLifecycle()

    SeasonBreakEditContent(
        season = season,
        seasonBreak = seasonBreak,
        seasonCompetitionState = seasonCompetitionState,
        onSave = { name, week ->
            seasonBreakViewModel.save(season.id, seasonBreak, name, week)
            appNavigator.goBack()
        },
        onConfirmSave = { name, week ->
            seasonBreakViewModel.save(season.id, seasonBreak, name, week)
        }
    )
}

@Composable
private fun SeasonBreakEditContent(
    season: Season,
    seasonBreak: SeasonBreak?,
    seasonCompetitionState: ViewModelState<SeasonCompView>,
    onSave: (String, Int) -> Unit,
    onConfirmSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(seasonBreak?.name ?: "") }
    var week by remember { mutableIntStateOf(seasonBreak?.week ?: 0) }

    ViewCommon(
        if (seasonBreak == null) "Add Season break" else "Edit Season break",
        description = "Return to season breaks",
        bottomBar = {
            BottomBarWithButton {
                onSave(name, week)
            }
        },
        confirm = {
            name.isNotEmpty() && (seasonBreak == null || name != seasonBreak.name) ||
                    (seasonBreak != null && week != seasonBreak.week)
        },
        confirmAction = { onConfirmSave(name, week) },
        states = persistentListOf(seasonCompetitionState)
    ) { paddingValues ->
        val range = buildDateRange(seasonCompetitionState.values().filter { it.seasonId == season.id })
        LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Name", "Week"))
            item { ViewTextField(name) { name = it } }
            item {
                DatePickerView(
                    week,
                    Modifier.padding(0.dp),
                    { utcMs -> DayDate.isMondayIn(range, DayDate(utcMs).value()) }) {
                    week = it
                }
            }
        }
    }
}

internal fun buildDateRange(seasonCompetitions: List<SeasonCompView>): IntRange {
    var startDate = 0
    var endDate = 0
    for ((_, _, _, _, _, startDate1, endDate1) in seasonCompetitions) {
        if (startDate1 > 0 && (startDate == 0 || startDate > startDate1)) {
            startDate = startDate1
        }
        if (endDate1 > 0 && (endDate == 0 || endDate < endDate1)) {
            endDate = endDate1
        }
    }
    return IntRange(startDate, endDate)
}
