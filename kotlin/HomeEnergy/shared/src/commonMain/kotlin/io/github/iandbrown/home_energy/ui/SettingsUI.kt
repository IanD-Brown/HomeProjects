package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.home_energy.database.Setting
import io.github.iandbrown.home_energy.database.SettingDao
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardType

internal class SettingsViewModel(dao: SettingDao) : CRUDViewModel<SettingDao, Setting>(dao = dao)

@Composable
internal fun SettingsRoute(navigate: (Setting?) -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()

    SettingsScreen(
        state = state,
        onAddSettings = { navigate(null) },
        onEditSettings = { navigate(it) },
        onDeleteSettings = { viewModel.delete(it) }
    )
}

@Composable
internal fun SettingsScreen(
    state: ViewModelState<Setting>,
    onAddSettings: () -> Unit,
    onEditSettings: (Setting) -> Unit,
    onDeleteSettings: (Setting) -> Unit
) {
    ViewCommon("Energy meters",
        persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                addButtonSettings(onAddSettings, state.values().isEmpty())
            )
        }) { padding ->
        LazyVerticalGrid(
            modifier = Modifier.padding(padding),
            columns = TrailingIconGridCells(0, 2)
        ) {
            viewTextItems(listOf("", ""))
            state.values().forEach {
                editButton { onEditSettings(it) }
                deleteButton { onDeleteSettings(it) }
            }
        }
    }
}

@Composable
internal fun SettingsEditorRoute(setting: Setting? = null, done: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    SettingsEditorScreen(
        setting = setting,
        onSave = { updatedSettings ->
            if (setting == null) {
                viewModel.insert(updatedSettings)
            } else {
                viewModel.update(updatedSettings)
            }
            done()
        }
    )
}

@Composable
internal fun SettingsEditorScreen(setting: Setting? = null, onSave: (Setting) -> Unit) {
    val title = if (setting == null) "New Setting" else "Edit Setting"
    var apiKey by remember { mutableStateOf(setting?.apiKey ?: "") }
    var apiPassword by remember { mutableStateOf(setting?.apiPassword ?: "") }
    var targetYear by remember { mutableIntStateOf(setting?.targetYear?.toInt() ?: 0) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if ((setting == null && apiKey.isEmpty() && apiPassword.isEmpty() && targetYear == 0) ||
            (setting != null && apiKey == setting.apiKey && apiPassword == setting.apiPassword && targetYear == setting.targetYear.toInt())
        ) {
            EditorState.CLEAN
        } else if (apiKey.isEmpty() || apiPassword.isEmpty() || targetYear != 0) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun toSetting() : Setting = Setting(apiKey = apiKey, apiPassword = apiPassword, targetYear = targetYear.toShort())

    ViewCommon(title,
        description = "Return to Energy settings screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                onSave(toSetting())
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { onSave(toSetting()) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row {
                ReadonlyViewText("API Key")
                ViewTextField(apiKey) {
                    apiKey = it
                    setEditorState()
                }
            }
            Row {
                ReadonlyViewText("API password")
                ViewTextField(apiPassword) {
                    apiPassword = it
                    setEditorState()
                }
            }
            Row {
                ReadonlyViewText("Target Year")
                TextField(
                    value = targetYear.toString(),
                    onValueChange = {
                        try {
                            targetYear = it.toInt()
                        } catch (e: NumberFormatException) {}
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors(),
                    textStyle = textStyle()
                )
            }
        }
    }
}
