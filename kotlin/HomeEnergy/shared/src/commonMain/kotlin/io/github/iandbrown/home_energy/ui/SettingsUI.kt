package io.github.iandbrown.home_energy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.iandbrown.home_energy.database.Setting
import io.github.iandbrown.home_energy.database.SettingDao
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel

internal val MONTHS = persistentListOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

internal class SettingsViewModel(dao: SettingDao) : CRUDViewModel<SettingDao, Setting>(dao = dao)

@Composable
internal fun SettingsEditorView(done: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val title = "Settings"
    var apiKey by remember { mutableStateOf( "") }
    var apiPassword by remember { mutableStateOf( "") }
    var targetYear by remember { mutableIntStateOf( 0) }
    var startMonth by remember { mutableIntStateOf( 0) }
    var initialBalance by remember { mutableDoubleStateOf( 0.0) }
    var directDebitAmount by remember { mutableDoubleStateOf( 0.0) }
    var fromYear by remember { mutableIntStateOf( 0) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }
    var loaded by remember { mutableStateOf(false) }

    fun setEditorState() {
        editorState = if ((state.values().isEmpty() && apiKey.isEmpty() && apiPassword.isEmpty() &&
                    targetYear == 0 && startMonth == 0 && initialBalance == 0.0 && directDebitAmount == 0.0 &&
                    fromYear == 0) ||
            (state.values().isNotEmpty()  && apiKey ==  state.values()[0].apiKey && apiPassword ==  state.values()[0].apiPassword &&
                    targetYear ==  state.values()[0].targetYear.toInt() && startMonth ==  state.values()[0].startMonth.toInt() &&
                    initialBalance ==  state.values()[0].initialBalance && directDebitAmount ==  state.values()[0].directDebitAmount &&
                    fromYear ==  state.values()[0].fromYear.toInt())
        ) {
            EditorState.CLEAN
        } else if (apiKey.isEmpty() || targetYear == 0 || fromYear == 0) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun toSetting() : Setting = Setting(apiKey = apiKey, apiPassword = apiPassword,
        targetYear = targetYear.toShort(), startMonth = startMonth.toShort(),
        initialBalance = initialBalance, directDebitAmount = directDebitAmount, fromYear = fromYear.toShort())

    fun save() {
        if (state.values().isEmpty()) {
            viewModel.insert(toSetting())
        } else {
            viewModel.update(toSetting())
        }
        done()
    }

    ViewCommon(title,
        description = "Return to Energy settings screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { save() }) { padding ->
        if (state.values().isNotEmpty() && !loaded) {
            loaded = true
            apiKey = state.values()[0].apiKey
            apiPassword = state.values()[0].apiPassword
            targetYear = state.values()[0].targetYear.toInt()
            startMonth = state.values()[0].startMonth.toInt()
            initialBalance = state.values()[0].initialBalance
            directDebitAmount = state.values()[0].directDebitAmount
            fromYear = state.values()[0].fromYear.toInt()
        }
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(apiKey, "API Key")  {
                apiKey = it
                setEditorState()
            }
            gridEntry(apiPassword, "API password") {
                apiPassword = it
                setEditorState()
            }
            gridEntry(targetYear.toString(), "Target Year") {
                targetYear = it.toInt()
                setEditorState()
            }
            gridEntry(MONTHS, startMonth, "Start Month") {
                startMonth = it
                setEditorState()
            }
            gridEntry(initialBalance.toString(), "Initial Balance") {
                initialBalance = it.toDouble()
                setEditorState()
            }
            gridEntry(directDebitAmount.toString(), "Direct Debit Amount") {
                directDebitAmount = it.toDouble()
                setEditorState()
            }
            gridEntry(fromYear.toString(), "From Year") {
                fromYear = it.toInt()
                setEditorState()
            }
        }
    }
}
