package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.reconciler.database.Account
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

private const val NAME = "name"

class AccountViewModel : BaseConfigCRUDViewModel<AccountDao, Account>(inject<AccountDao>().value)

@Suppress("ParamsComparedByRef")
@Composable
internal fun AccountListView(viewModel: AccountViewModel = koinInject<AccountViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Accounts",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch {
                    importCsvFile(inject<AccountDao>().value) { toAccount(it) }
                } },
                ButtonSettings("Export") { coroutineScope.launch {
                    exportToFile("accounts") { toDataFrame(state.value).writeCsv(it) }
                }},
                ButtonSettings("+") { it.navigate(Account(name = "")) })
        }) { paddingValues ->
        LazyVerticalGrid(
            columns = WeightedIconGridCells(2, 1),
            Modifier.padding(paddingValues)
        ) {
            item { ViewText("Name") }
            item(span = { GridItemSpan(2) }) {}
            for (account in state.value) {
                item { ViewText(account.name) }
                item { EditButton { navController -> navController.navigate(account) } }
                item { DeleteButton { viewModel.delete(account) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditAccount(account: Account,
                                     viewModel: AccountViewModel = koinInject<AccountViewModel>()) {
    var name by remember { mutableStateOf(account.name) }
    val title = if (account.id == 0) "Add Account" else "Edit Account"
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = if (account.id == 0) {
            if (name.isNotEmpty()) EditorState.VALID else EditorState.DIRTY
        } else if (name == account.name) {
            EditorState.CLEAN
        } else if (name.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    ViewCommon(
        title,
        description = "Return to Accounts",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save(account, viewModel, name)
                it.popBackStack()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save(account, viewModel, name)}) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.padding(paddingValues)) {
            gridEntry("Name", name) { name = it
                setEditorState()
            }
        }
    }
}

private fun save(account: Account?, viewModel: AccountViewModel, name: String) {
    if (account == null || account.id == 0) {
        viewModel.insert(Account(name = name))
    } else {
        viewModel.update(Account(account.id, name))
    }
}

internal fun toDataFrame(accounts: List<Account>): DataFrame<Account> =
    accounts.toDataFrame {
        NAME from { it.name }
    }

internal fun toAccount(row: DataRow<Any?>): Account = Account(name = row[NAME] as String)
