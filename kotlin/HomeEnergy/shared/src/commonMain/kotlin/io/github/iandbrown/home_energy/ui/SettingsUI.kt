package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import io.github.iandbrown.home_energy.database.Setting
import io.github.iandbrown.home_energy.database.SettingDao
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel

internal val MONTHS = persistentListOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

internal class SettingsViewModel(dao: SettingDao) : CRUDViewModel<SettingDao, Setting>(dao = dao)

@Composable
internal fun SettingsRoute(navigate: (Setting?) -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    ViewCommon("Settings",
        persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                addButtonSettings({ navigate(null) }, state.values().isEmpty())
            )
        }) { padding ->
        TrailingIconLazyVerticalGrid(padding, 1, 2) {
            viewTextItems(listOf("", "", ""))
            state.values().forEach {
                viewTextItems(listOf("Settings"))
                editButton { navigate(it) }
                deleteButton { viewModel.delete(it) }
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
    var startMonth by remember { mutableIntStateOf(setting?.startMonth?.toInt() ?: 0) }
    var initialBalance by remember { mutableDoubleStateOf(setting?.initialBalance ?: 0.0) }
    var directDebitAmount by remember { mutableDoubleStateOf(setting?.directDebitAmount ?: 0.0) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if ((setting == null && apiKey.isEmpty() && apiPassword.isEmpty() &&
                    targetYear == 0 && startMonth == 0 && initialBalance == 0.0 && directDebitAmount == 0.0) ||
            (setting != null && apiKey == setting.apiKey && apiPassword == setting.apiPassword &&
                    targetYear == setting.targetYear.toInt() && startMonth == setting.startMonth.toInt() &&
                    initialBalance == setting.initialBalance && directDebitAmount == setting.directDebitAmount)
        ) {
            EditorState.CLEAN
        } else if (apiKey.isEmpty() || targetYear == 0) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun toSetting() : Setting = Setting(apiKey = apiKey, apiPassword = apiPassword,
        targetYear = targetYear.toShort(), startMonth = startMonth.toShort(), initialBalance = initialBalance, directDebitAmount = directDebitAmount)

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
            EditorRow("API Key") {
                ViewTextField(apiKey) {
                    apiKey = it
                    setEditorState()
                }
            }
            EditorRow("API password") {
                ViewTextField(apiPassword) {
                    apiPassword = it
                    setEditorState()
                }
            }
            EditorRow("Target Year") {
                NumericField(targetYear.toString()) {
                    targetYear = it.toInt()
                    setEditorState()
                }
            }
            EditorRow("Start Month") {
                DropdownList(MONTHS, startMonth) {
                    startMonth = it
                    setEditorState()
                }
            }
            EditorRow("Initial Balance") {
                NumericField(initialBalance.toString()) {
                    initialBalance = it.toDouble()
                    setEditorState()
                }
            }
            EditorRow("Direct Debit Amount") {
                NumericField(directDebitAmount.toString()) {
                    directDebitAmount = it.toDouble()
                    setEditorState()
                }
            }
        }
    }
}
