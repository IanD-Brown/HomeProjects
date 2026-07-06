package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.home_energy.database.Usage
import io.github.iandbrown.home_energy.database.UsageDao
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel

internal class UsageViewModel(usageDao: UsageDao) : CRUDViewModel<UsageDao, Usage>(dao = usageDao)

@Composable
internal fun UsageRoute(viewModel: UsageViewModel = koinViewModel()) {
    val state by viewModel.getState().collectAsState()
    UsageScreen(state = state)
}

@Composable
internal fun UsageScreen(state: ViewModelState<Usage>) {
    ViewCommon("Energy usage",
        persistentListOf(state),
        bottomBar = {
        }) { padding ->
        LazyVerticalGrid(modifier = Modifier.padding(padding),
            columns = TrailingIconGridCells(6, 0)) {
            viewTextItems(listOf("Year", "Month", "Day", "Period", "Meter", "Amount"))
            state.values().forEach {
                viewTextItems(listOf(it.year.toString(),
                    it.month.toString(),
                    it.day.toString(),
                    it.period.toString(),
                    it.meterPointAdminNumber,
                    it.consumption.toString()))
            }
        }
    }
}
