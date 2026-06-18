package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.reconciler.database.Account
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.database.AccountGroupDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

private const val NAME = "name"

class AccountViewModel : BaseConfigCRUDViewModel<AccountDao, Account>(inject<AccountDao>().value)

@Suppress("ParamsComparedByRef")
@Composable
internal fun AccountListView(viewModel: AccountViewModel = koinInject<AccountViewModel>(),
                             accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Accounts",
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "accounts") { writer ->
                    val groupLookup = accountGroupState.values().associateBy ({ it.id }, {it.name} )
                    toDataFrame(state.values(), groupLookup).writeCsv(writer)
                },
                importCsvButtonSettings(viewModel) { toAccount(it) },
                addButtonSettings { it.navigate(Account(name = "", accountGroup = 0)) })
        },
        states = persistentListOf(state.value, accountGroupState.value)) { paddingValues ->
        LazyVerticalGrid(
            columns = WeightedIconGridCells(2, 1, 1),
            Modifier.padding(paddingValues)
        ) {
            val groupLookup = accountGroupState.values().associateBy ({ it.id }, {it.name} )
            viewTextItems(values = listOf("Name", "Group"))
            item(span = { GridItemSpan(2) }) {}
            for (account in state.values()) {
                viewTextItems(values = listOf(account.name, groupLookup[account.accountGroup] ?: ""))
                item { EditButton { it.navigate(account) } }
                item { DeleteButton { viewModel.delete(account) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditAccount(account: Account,
                         viewModel: AccountViewModel = koinInject(),
                         accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(account.name) }
    var group by remember { mutableIntStateOf(account.accountGroup) }
    val title = if (account.id == 0) "Add Account" else "Edit Account"
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        editorState = when {
            account.id == 0 && name.isNotEmpty() -> EditorState.VALID
            account.id == 0 && name.isEmpty() && group == account.accountGroup -> EditorState.CLEAN
            account.id != 0 && name.isNotEmpty() && (name != account.name || account.accountGroup != group) -> EditorState.VALID
            account.id != 0 && (name != account.name || group != account.accountGroup) -> EditorState.DIRTY
            account.id != 0 -> EditorState.CLEAN
            else -> EditorState.DIRTY
        }
    }

    ViewCommon(
        title,
        description = "Return to Accounts",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save(account, viewModel, name, group)
                it.popBackStack()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save(account, viewModel, name, group)},
        states = persistentListOf(accountGroupState.value)) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.padding(paddingValues)) {
            gridEntry("Name", name) { name = it
                setEditorState()
            }
            val data = accountGroupState.values()
            gridEntry("Group",
                data.map { it.name }.toImmutableList(),
                data.map {it.id}.indexOf(group)) {
                group = data.map {accountGroup ->  accountGroup.id}[it]
                setEditorState()
            }
        }
    }
}

private fun save(account: Account?, viewModel: AccountViewModel, name: String, group: Int) {
    if (account == null || account.id == 0) {
        viewModel.insert(Account(name = name, accountGroup = group))
    } else {
        viewModel.update(Account(account.id, name, group))
    }
}

internal fun toDataFrame(accounts: List<Account>, groupLookup: Map<Int, String>): DataFrame<Account> =
    accounts.toDataFrame {
        NAME from { it.name }
        ACCOUNT_GROUP from { groupLookup[it.accountGroup]!! }
    }

internal suspend fun toAccount(row: DataRow<Any?>, accountGroupDao: AccountGroupDao = inject<AccountGroupDao>().value): Account =
    Account(name = row[NAME] as String, accountGroup = accountGroupDao.getByName(row[ACCOUNT_GROUP] as String)!!)
