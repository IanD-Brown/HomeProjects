package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
internal fun MeterRoute(showMeterEditor: (Meter?) -> Unit, editTariff: (Meter?) -> Unit) {
    val viewModel: MeterViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val settingsModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsModel.getState().collectAsState()

    ViewCommon("Energy meters",
        persistentListOf(state, settingsState),
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(enabled = !settingsState.values().isEmpty(), imageVector = Icons.AutoMirrored.Filled.ReadMore) {
                    viewModel.readConsumption(state.values())
                },
                addButtonSettings({
                    showMeterEditor(null)
                    viewModel.readAll()
                })
            )
        }) { padding ->
        TrailingIconLazyVerticalGrid(padding, 3, 3) {
            viewTextItems(listOf("Name", "Meter Point Admin Number", "Serial", "", "", ""))
            state.values().forEach {
                viewTextItems(listOf(it.name, it.meterPointAdminNumber, it.serial))
                clickableIcon(Icons.Default.ShoppingCart, "Tariff", Color.Green) { editTariff(it) }
                editButton {
                    showMeterEditor(it)
                    viewModel.readAll()
                }
                deleteButton { viewModel.delete(it) }
            }
        }
    }
}

@Composable
internal fun MeterEditorRoute(meter: Meter? = null, done: () -> Unit) {
    val viewModel: MeterViewModel = koinViewModel()
    val title = if (meter == null) "New Meter" else "Edit Meter"
    var meterPointAdminNumber by remember { mutableStateOf(meter?.meterPointAdminNumber ?: "") }
    var serial by remember { mutableStateOf(meter?.serial ?: "") }
    var electric by remember { mutableStateOf(meter?.electric ?: true) }
    var standingCharge by remember { mutableDoubleStateOf(meter?.standingCharge ?: 0.0) }
    var name by remember { mutableStateOf(meter?.name ?: "") }
    var comparisonStandingCharge by remember { mutableDoubleStateOf(meter?.compareStandingCharge ?: 0.0) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if ((meter == null && meterPointAdminNumber.isEmpty() && serial.isEmpty() &&
                    electric && standingCharge == 0.0 && name.isEmpty() && comparisonStandingCharge == 0.0) ||
            (meter != null && meterPointAdminNumber == meter.meterPointAdminNumber &&
                    serial == meter.serial && electric == meter.electric &&
                    standingCharge == meter.standingCharge && name == meter.name && comparisonStandingCharge == meter.compareStandingCharge)
        ) {
            EditorState.CLEAN
        } else if (meterPointAdminNumber.isEmpty() || serial.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun save() {
        if (meter == null) {
            viewModel.insert(Meter(meterPointAdminNumber, serial, electric, standingCharge, name = name, compareStandingCharge = comparisonStandingCharge))
        } else {
            viewModel.update(Meter(meterPointAdminNumber, serial, electric, standingCharge, meter.id, name, comparisonStandingCharge))
        }
    }

    ViewCommon(title,
        description = "Return to Energy meters screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save()
                done()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { save() }) { padding ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(padding)) {
            gridEntry(name, "Name") {
                name = it
                setEditorState()
            }
            gridEntry(meterPointAdminNumber, "Meter Point Admin Number") {
                meterPointAdminNumber = it
                setEditorState()
            }
            gridEntry(serial, "Serial") {
                serial = it
                setEditorState()
            }
            gridEntry(electric, "Electric") {
                electric = it
                setEditorState()
            }
            gridEntry(standingCharge, "Standing charge") {
                standingCharge = it.toDouble()
                setEditorState()
            }
            gridEntry(comparisonStandingCharge, "ComparisonStanding charge") {
                comparisonStandingCharge = it.toDouble()
                setEditorState()
            }
        }
    }
}
