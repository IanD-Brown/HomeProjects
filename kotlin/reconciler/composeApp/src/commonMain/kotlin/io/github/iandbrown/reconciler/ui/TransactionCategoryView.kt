package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import io.github.iandbrown.reconciler.database.AccountGroupDao
import io.github.iandbrown.reconciler.database.TransactionCategory
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

private const val NAME = "name"
private const val FILTER = "filter"
private const val IS_SPENDING = "isSpending"

class TransactionCategoryViewModel :
    BaseConfigCRUDViewModel<TransactionCategoryDao, TransactionCategory>(inject<TransactionCategoryDao>().value)

@Suppress("ParamsComparedByRef")
@Composable
internal fun TransactionCategoryListView(viewModel: TransactionCategoryViewModel = koinInject(),
                                         accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var accountGroup by remember { mutableIntStateOf(-1) }

    ViewCommon(
        "Transaction Categories",
        bottomBar = {
            BottomBarWithButtons(
                importCsvButtonSettings(viewModel) { toTransactionCategory(it) },
                exportButtonSettings(coroutineScope, "transactionCategories") { writer ->
                    val groupLookup = accountGroupState.value.values().associateBy ({ it.id }, {it.name} )
                    toDataFrame(state.value.values(), groupLookup).writeCsv(writer)
                },
                ButtonSettings("+") { TransactionCategory(name = "", filter = false, accountGroup = accountGroup) })
        },
        states = persistentListOf(state.value, accountGroupState.value)) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ViewText("Account Group")
                Spacer(modifier = Modifier.size(16.dp))
                val value = accountGroupState.value.values()
                DropdownList(
                    value.map { it.name }.toImmutableList(),
                    value.map { it.id }.indexOf(accountGroup)
                ) {
                    accountGroup = value[it].id
                }
            }

            LazyVerticalGrid(columns = WeightedIconGridCells(2, 2, 1, 1)) {
                viewTextItems("Name", "Filter", "Is Spending")
                item(span = { GridItemSpan(2) }) {}
                for (transactionCategory in state.value.values().filter { it.accountGroup == accountGroup }) {
                    viewTextItems(
                        transactionCategory.name,
                        transactionCategory.filter.toString(),
                        transactionCategory.isSpending.toString()
                    )
                    item { EditButton { navController -> navController.navigate(transactionCategory) } }
                    item { DeleteButton { viewModel.delete(transactionCategory) } }
                }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditTransactionCategory(
    transactionCategory: TransactionCategory,
    viewModel: TransactionCategoryViewModel = koinInject(),
    accountGroupViewModel: AccountGroupViewModel = koinInject()
) {
    var name by remember { mutableStateOf(transactionCategory.name) }
    var filter by remember { mutableStateOf(transactionCategory.filter) }
    var isSpending by remember { mutableStateOf(transactionCategory.isSpending) }
    var accountGroup by remember { mutableIntStateOf(transactionCategory.accountGroup) }
    val title = if (transactionCategory.id == 0) "Add Transaction Category" else "Edit Transaction Category"
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()

    fun setEditorState() {
        editorState = if (transactionCategory.id == 0) {
            if (name.isNotEmpty()) EditorState.VALID else EditorState.DIRTY
        } else if (name == transactionCategory.name && filter == transactionCategory.filter && isSpending == transactionCategory.isSpending) {
            EditorState.CLEAN
        } else if (name.isEmpty()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    ViewCommon(
        title,
        description = "Return to Transaction Categories",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save(transactionCategory, viewModel, name, filter, isSpending, accountGroup)
                it.popBackStack()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { save(transactionCategory, viewModel, name, filter, isSpending, accountGroup) },
        states = persistentListOf(accountGroupState.value)) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.padding(paddingValues)) {
            gridEntry("Name", name) {
                name = it
                setEditorState()
            }

            gridEntry("Filter", filter) {
                filter = it
                setEditorState()
            }

            gridEntry("Is Spending", isSpending) {
                isSpending = it
                setEditorState()
            }
            val accountGroupName =
                accountGroupState.value.values()
                    .filter { it.id == transactionCategory.accountGroup }
                    .map { it.name }
                    .firstOrNull()
            viewTextItems("Account Group", accountGroupName ?: "")
        }
    }
}

private fun save(
    transactionCategory: TransactionCategory?,
    viewModel: TransactionCategoryViewModel,
    name: String,
    filter: Boolean,
    isSpending: Boolean,
    accountGroup: Int
) {
    if (transactionCategory == null || transactionCategory.id == 0) {
        viewModel.insert(TransactionCategory(name = name, filter = filter, isSpending = isSpending, accountGroup = accountGroup))
    } else {
        viewModel.update(TransactionCategory(transactionCategory.id, name, filter, isSpending, accountGroup))
    }
}

internal fun toDataFrame(transactionCategories: List<TransactionCategory>, groupLookup: Map<Int, String>): DataFrame<TransactionCategory> =
    transactionCategories.toDataFrame {
        NAME from { it.name }
        FILTER from { it.filter }
        IS_SPENDING from { it.isSpending }
        ACCOUNT_GROUP from { groupLookup[it.accountGroup]!! }
    }

internal suspend fun toTransactionCategory(row: DataRow<Any?>, accountGroupDao: AccountGroupDao = inject<AccountGroupDao>().value): TransactionCategory =
    TransactionCategory(
        name = row[NAME] as String,
        filter = row[FILTER] as Boolean,
        isSpending = row[IS_SPENDING] as Boolean,
        accountGroup = accountGroupDao.getByName(row[ACCOUNT_GROUP] as String)!!
    )
