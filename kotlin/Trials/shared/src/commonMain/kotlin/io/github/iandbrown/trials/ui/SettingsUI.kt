package io.github.iandbrown.trials.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.iandbrown.trials.database.Setting
import io.github.iandbrown.trials.database.SettingDao
import org.koin.compose.viewmodel.koinViewModel

internal class SettingsViewModel(dao: SettingDao) : CRUDViewModel<SettingDao, Setting>(dao = dao)

@Composable
internal fun SettingsEditorView(done: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val title = "Settings"
    var smtpHost by remember { mutableStateOf( "") }
    var userName by remember { mutableStateOf( "") }
    var password by remember { mutableStateOf( "") }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }
    var loaded by remember { mutableStateOf(false) }

    fun setEditorState() {
        editorState = if ((state.values().isEmpty() && smtpHost.isEmpty() && userName.isEmpty() && password.isEmpty()) ||
            (state.values().isNotEmpty()  && smtpHost ==  state.values()[0].smtpHost && userName ==  state.values()[0].userName && password ==  state.values()[0].password)
        ) {
            EditorState.CLEAN
        } else if (smtpHost.isEmpty() || userName.isEmpty() || password.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun toSetting() : Setting = Setting(smtpHost = smtpHost, userName = userName, password = password)

    fun save() {
        if (state.values().isEmpty()) {
            viewModel.insert(toSetting())
        } else {
            viewModel.update(toSetting())
        }
        done()
    }

    ViewCommon(title,
        description = "Return to Home screen",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { save() }) { padding ->
        if (state.values().isNotEmpty() && !loaded) {
            loaded = true
            smtpHost = state.values()[0].smtpHost
            userName = state.values()[0].userName
            password = state.values()[0].password
        }
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(smtpHost, "SMTP Host")  {
                smtpHost = it
                setEditorState()
            }
            gridEntry(userName, "User Name") {
                userName = it
                setEditorState()
            }
            gridEntry(password, "Password") {
                password = it
                setEditorState()
            }
        }
    }
}
