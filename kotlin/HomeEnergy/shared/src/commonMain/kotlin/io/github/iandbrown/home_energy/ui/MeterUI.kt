package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.MeterDao
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel

private data object Screen

private data class Editor(val entity: Meter? = null)

@Composable
fun MeterActivity() {
    val backStack = remember { mutableStateListOf<Any>(Screen) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Screen -> NavEntry(key) {
                    MeterScreen(navigate = {
                        backStack.add(Editor(it))
                    })
                }
                is Editor -> NavEntry(key) {
                    MeterEditor(key.entity, done = {backStack.removeLastOrNull()})
                }
                else -> {
                    error("Unknown route: $key")
                }
            }
        }
    )
}

internal class MeterViewModel(dao: MeterDao) : CRUDViewModel<MeterDao, Meter>(dao = dao)

@Suppress("ParamsComparedByRef")
@Composable
internal fun MeterScreen(
    navigate : (Meter?) -> Unit,
    viewModel: MeterViewModel = koinViewModel()) {
    val state = viewModel.getState().collectAsState()

    ViewCommon("Energy meters",
        persistentListOf(state.value),
        bottomBar = {
            BottomBarWithButtons(
                addButtonSettings { navigate(null) }
            )
        }) { padding ->
        LazyVerticalGrid(modifier = Modifier.padding(padding),
            columns = TrailingIconGridCells(2, 2)) {
            viewTextItems(listOf("Meter Point Admin Number", "Serial", "", ""))
            state.values().forEach {
                viewTextItems(listOf(it.meterPointAdminNumber, it.serial))
                editButton { navigate(it) }
                deleteButton { viewModel.delete(it) }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun MeterEditor(meter: Meter? = null, done : () -> Unit, viewModel: MeterViewModel = koinViewModel()) {
    val title = if (meter == null) "New Meter" else "Edit Meter"
    var meterPointAdminNumber by remember { mutableStateOf(meter?.meterPointAdminNumber ?: "") }
    var serial by remember { mutableStateOf(meter?.serial ?: "") }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if ((meter == null && meterPointAdminNumber.isEmpty() && serial.isEmpty()) ||
            (meter != null && meterPointAdminNumber == meter.meterPointAdminNumber && serial == meter.serial)) {
            EditorState.CLEAN
        } else if (meterPointAdminNumber.isEmpty() || serial.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun save() {
        if (meter == null) {
            viewModel.insert(Meter(meterPointAdminNumber, serial))
        } else {
            viewModel.update(meter.copy(meterPointAdminNumber = meterPointAdminNumber, serial = serial))
        }
        done()
    }

    ViewCommon(title,
        description = "Return to Energy meters screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save()}) { padding ->
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
        }
    }
}
