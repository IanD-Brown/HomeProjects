package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.iandbrown.reconciler.database.AccountGroup
import io.github.iandbrown.reconciler.database.AccountGroupDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

private const val NAME = "name"

class AccountGroupViewModel : BaseConfigCRUDViewModel<AccountGroupDao, AccountGroup>(inject<AccountGroupDao>().value)

@Suppress("ParamsComparedByRef")
@Composable
internal fun AccountGroupListView(viewModel: AccountGroupViewModel = koinInject<AccountGroupViewModel>()) {
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Account Groups",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch {
                    importCsvFile(inject<AccountGroupDao>().value) { toAccountGroup(it) }
                } },
                ButtonSettings("Export") { coroutineScope.launch {
                    exportToFile("accountGroups") { toDataFrame(state.value.values()).writeCsv(it) }
                }},
                ButtonSettings("+") { it.navigate(AccountGroup(name = "")) })
        },
        states = persistentListOf(state.value)) { paddingValues ->
        LazyVerticalGrid(
            columns = WeightedIconGridCells(3, 1),
            Modifier.padding(paddingValues)
        ) {
            item { ViewText("Name") }
            item(span = { GridItemSpan(3) }) {}
            for (accountGroup in state.value.values()) {
                item { ViewText(accountGroup.name) }
                item { Icon(Icons.Default.Upload, "export",
                    Modifier.clickable(onClick = { coroutineScope.launch { export(accountGroup.id)}}), Color.Green)
                }
                item { EditButton { navController -> navController.navigate(accountGroup) } }
                item { DeleteButton { viewModel.delete(accountGroup) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditAccountGroup(accountGroup: AccountGroup,
                                     viewModel: AccountGroupViewModel = koinInject<AccountGroupViewModel>()) {
    var name by remember { mutableStateOf(accountGroup.name) }
    val title = if (accountGroup.id == 0) "Add Account Group" else "Edit Account Group"
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if (accountGroup.id == 0) {
            if (name.isNotEmpty()) EditorState.VALID else EditorState.DIRTY
        } else if (name == accountGroup.name) {
            EditorState.CLEAN
        } else if (name.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    ViewCommon(
        title,
        description = "Return to AccountGroups",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save(accountGroup, viewModel, name)
                it.popBackStack()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save(accountGroup, viewModel, name)},
        states = persistentListOf()) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.padding(paddingValues)) {
            gridEntry("Name", name) { name = it
                setEditorState()
            }
        }
    }
}

private fun save(accountGroup: AccountGroup?, viewModel: AccountGroupViewModel, name: String) {
    if (accountGroup == null || accountGroup.id == 0) {
        viewModel.insert(AccountGroup(name = name))
    } else {
        viewModel.update(AccountGroup(accountGroup.id, name))
    }
}

internal fun toDataFrame(accountGroups: List<AccountGroup>): DataFrame<AccountGroup> =
    accountGroups.toDataFrame {
        NAME from { it.name }
    }

internal fun toAccountGroup(row: DataRow<Any?>): AccountGroup = AccountGroup(name = row[NAME] as String)
