package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
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

    ViewCommon("Energy meters",
        persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(imageVector = Icons.AutoMirrored.Filled.ReadMore, onClick = { viewModel.readConsumption(state.values()) }),
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
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if ((meter == null && meterPointAdminNumber.isEmpty() && serial.isEmpty() &&
                    electric && standingCharge == 0.0 && name.isEmpty()) ||
            (meter != null && meterPointAdminNumber == meter.meterPointAdminNumber &&
                    serial == meter.serial && electric == meter.electric &&
                    standingCharge == meter.standingCharge && name == meter.name)
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
            viewModel.insert(Meter(meterPointAdminNumber, serial, electric, standingCharge, name = name))
        } else {
            viewModel.update(Meter(meterPointAdminNumber, serial, electric, standingCharge, meter.id, name))
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
        Column(modifier = Modifier.padding(padding)) {
            EditorRow("Meter Point Admin Number") {
                ViewTextField(meterPointAdminNumber) {
                    meterPointAdminNumber = it
                    setEditorState()
                }
            }
            EditorRow("Serial") {
                ViewTextField(serial) {
                    serial = it
                    setEditorState()
                }
            }
            EditorRow("Electric") {
                Checkbox(
                    checked = electric,
                    onCheckedChange = {
                        electric = it
                        setEditorState()
                    }
                )
            }
            EditorRow("Standing charge") {
                NumericField(standingCharge.toString()) {
                    standingCharge = it.toDouble()
                    setEditorState()
                }
            }
            EditorRow("Name") {
                ViewTextField(name) {
                    name = it
                    setEditorState()
                }
            }
        }
    }
}
