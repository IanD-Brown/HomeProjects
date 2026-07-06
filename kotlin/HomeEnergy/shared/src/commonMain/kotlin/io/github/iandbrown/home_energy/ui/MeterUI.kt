package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.MeterDao
import io.github.iandbrown.home_energy.repository.MeterRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

internal class MeterViewModel(dao: MeterDao, private val repository: MeterRepository) : CRUDViewModel<MeterDao, Meter>(dao = dao) {
    fun readConsumption(meters: List<Meter>) {
        viewModelScope.launch {
            try {
                setLoading()
                repository.syncConsumption(meters)
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}

@Composable
internal fun MeterRoute(
    navigate: (Meter?) -> Unit,
    viewModel: MeterViewModel = koinViewModel()
) {
    val state by viewModel.getState().collectAsState()

    MeterScreen(
        state = state,
        onReadConsumption = { viewModel.readConsumption(state.values()) },
        onAddMeter = { navigate(null) },
        onEditMeter = { navigate(it) },
        onDeleteMeter = { viewModel.delete(it) }
    )
}

@Composable
internal fun MeterScreen(
    state: ViewModelState<Meter>,
    onReadConsumption: () -> Unit,
    onAddMeter: () -> Unit,
    onEditMeter: (Meter) -> Unit,
    onDeleteMeter: (Meter) -> Unit
) {
    ViewCommon("Energy meters",
        persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(imageVector = Icons.AutoMirrored.Filled.ReadMore, onClick = onReadConsumption),
                addButtonSettings(onAddMeter)
            )
        }) { padding ->
        LazyVerticalGrid(
            modifier = Modifier.padding(padding),
            columns = TrailingIconGridCells(2, 2)
        ) {
            viewTextItems(listOf("Meter Point Admin Number", "Serial", "", ""))
            state.values().forEach {
                viewTextItems(listOf(it.meterPointAdminNumber, it.serial))
                editButton { onEditMeter(it) }
                deleteButton { onDeleteMeter(it) }
            }
        }
    }
}

@Composable
internal fun MeterEditorRoute(
    meter: Meter? = null,
    done: () -> Unit,
    viewModel: MeterViewModel = koinViewModel()
) {
    MeterEditorScreen(
        meter = meter,
        onSave = { updatedMeter ->
            if (meter == null) {
                viewModel.insert(updatedMeter)
            } else {
                viewModel.update(updatedMeter)
            }
            done()
        }
    )
}

@Composable
internal fun MeterEditorScreen(
    meter: Meter? = null,
    onSave: (Meter) -> Unit
) {
    val title = if (meter == null) "New Meter" else "Edit Meter"
    var meterPointAdminNumber by remember { mutableStateOf(meter?.meterPointAdminNumber ?: "") }
    var serial by remember { mutableStateOf(meter?.serial ?: "") }
    var electric by remember { mutableStateOf(meter?.electric ?: true) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if ((meter == null && meterPointAdminNumber.isEmpty() && serial.isEmpty() && electric) ||
            (meter != null && meterPointAdminNumber == meter.meterPointAdminNumber && serial == meter.serial && electric == meter.electric)
        ) {
            EditorState.CLEAN
        } else if (meterPointAdminNumber.isEmpty() || serial.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    ViewCommon(title,
        description = "Return to Energy meters screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                onSave(Meter(meterPointAdminNumber, serial, electric))
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { onSave(Meter(meterPointAdminNumber, serial, electric)) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row {
                ReadonlyViewText("Meter Point Admin Number")
                ViewTextField(meterPointAdminNumber) {
                    meterPointAdminNumber = it
                    setEditorState()
                }
            }
            Row {
                ReadonlyViewText("Serial")
                ViewTextField(serial) {
                    serial = it
                    setEditorState()
                }
            }
            Row {
                ReadonlyViewText("Electric")
                Checkbox(
                    checked = electric,
                    onCheckedChange = {
                        electric = it
                        setEditorState()
                    }
                )
            }
        }
    }
}
