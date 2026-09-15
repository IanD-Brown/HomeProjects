package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.MeterTariff
import io.github.iandbrown.home_energy.database.MeterTariffDao
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal class MeterTariffViewModel(meterId: Int, dao: MeterTariffDao) :
    CRUDViewModel<MeterTariffDao, MeterTariff>(dao, { dao.get(meterId) })

@Composable
internal fun MeterTariffListRoute(
    meter: Meter,
    viewModel: MeterTariffViewModel = koinViewModel(key = "TariffList_${meter.id}") { parametersOf(meter.id) },
    editTariff: (Int, MeterTariff?) -> Unit
) {
    val state by viewModel.getState().collectAsState()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    MeterTariffListScreen(
        meter = meter,
        state = state,
        onAddTariff = { editTariff(meter.id, null) },
        onEditTariff = { tariff -> editTariff(meter.id, tariff) },
        onDeleteTariff = { viewModel.delete(it) }
    )
}

@Composable
internal fun MeterTariffListScreen(
    meter: Meter,
    state: ViewModelState<MeterTariff>,
    onAddTariff: () -> Unit,
    onEditTariff: (MeterTariff) -> Unit,
    onDeleteTariff: (MeterTariff) -> Unit
) {
    fun asHoursAndMinutes(hour: Short, period: Short): String {
        return "${hour.toString().padStart(2, '0')}:${period.times(30).toString().padStart(2, '0')}"
    }

    ViewCommon("Meter Tariff ${meter.meterPointAdminNumber}",
        states = persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                addButtonSettings(onAddTariff)
            )
        }) { padding ->
        TrailingIconLazyVerticalGrid(padding, 4, 2) {
            viewTextItems(listOf("Active", "From", "To", "Rate", "", ""))
            state.values().forEach {
                gridEntry(it.activeAccount, null, false) {}
                viewTextItems(
                    listOf(
                        asHoursAndMinutes(it.fromHour, it.fromPeriod),
                        asHoursAndMinutes(it.toHour, it.toPeriod),
                        it.tariff.toString()
                    )
                )
                editButton { onEditTariff(it) }
                deleteButton { onDeleteTariff(it) }
            }
        }
    }
}

@Composable
internal fun MeterTariffEditorRoute(
    meterId: Int,
    meterTariff: MeterTariff?,
    viewModel: MeterTariffViewModel = koinViewModel(key = "TariffEditor_$meterId") { parametersOf(meterId) },
    done: () -> Unit
) {
    MeterTariffEditorScreen(
        meterId = meterId,
        meterTariff = meterTariff,
        onSave = { updatedTariff ->
            if (meterTariff == null) {
                viewModel.insert(updatedTariff)
            } else {
                viewModel.update(updatedTariff)
            }
            done()
        }
    )
}

@Composable
internal fun MeterTariffEditorScreen(
    meterId: Int,
    meterTariff: MeterTariff?,
    onSave: (MeterTariff) -> Unit
) {
    val title = if (meterTariff == null) "New Meter Tariff" else "Edit Meter Tariff"
    var fromHour by remember { mutableIntStateOf(meterTariff?.fromHour?.toInt() ?: 0) }
    var fromPeriod by remember { mutableIntStateOf(meterTariff?.fromPeriod?.toInt() ?: 0) }
    var toHour by remember { mutableIntStateOf(meterTariff?.toHour?.toInt() ?: 0) }
    var toPeriod by remember { mutableIntStateOf(meterTariff?.toPeriod?.toInt() ?: 0) }
    var tariff by remember { mutableDoubleStateOf(meterTariff?.tariff ?: 0.0) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }
    var activeTariff by remember { mutableStateOf(meterTariff?.activeAccount ?: true) }

    fun setEditorState() {
        editorState = if ((meterTariff == null && fromHour == 0 && fromPeriod == 0 && toHour == 0 && toPeriod == 0 && tariff == 0.0) ||
            (meterTariff != null && fromHour == meterTariff.fromHour.toInt() &&
                    fromPeriod == meterTariff.fromPeriod.toInt() && toHour == meterTariff.toHour.toInt() &&
                    toPeriod == meterTariff.toPeriod.toInt() && tariff == meterTariff.tariff)
        ) {
            EditorState.CLEAN
        } else if ((fromHour * 24 + fromPeriod) >= (toHour * 24 + toPeriod) || toHour == 24 && toPeriod != 0 || tariff == 0.0) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    val hours = persistentListOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24")
    ViewCommon(title,
        description = "Return to Meter tariffs screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                onSave(MeterTariff(meterId, fromHour.toShort(), fromPeriod.toShort(), toHour.toShort(), toPeriod.toShort(), tariff, activeTariff, meterTariff?.id ?: 0))
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { onSave(MeterTariff(meterId, fromHour.toShort(), fromPeriod.toShort(), toHour.toShort(), toPeriod.toShort(), tariff, activeTariff, meterTariff?.id ?: 0)) }) { padding ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(padding)) {
            gridEntry(hours, fromHour, "From Hour") {
                fromHour = it
                setEditorState()
            }
            gridEntry(persistentListOf("00", "30"), fromPeriod, "From Period") {
                fromPeriod = it
                setEditorState()
            }
            gridEntry(hours, toHour, "To Hour") {
                toHour = it
                setEditorState()
            }
            gridEntry(persistentListOf("00", "30"), toPeriod, "To Period") {
                toPeriod = it
                setEditorState()
            }
            gridEntry(tariff, "Tariff") {
                tariff = it.toDouble()
                setEditorState()
            }
            gridEntry(activeTariff, "Active tariff", meterTariff == null) {
                activeTariff = it
                setEditorState()
            }
        }
    }
}
