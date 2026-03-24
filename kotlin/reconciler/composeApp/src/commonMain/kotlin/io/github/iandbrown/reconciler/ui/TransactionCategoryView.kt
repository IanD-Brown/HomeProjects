package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.reconciler.database.TransactionCategory
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.koin.compose.koinInject

private const val NAME = "name"
private const val FILTER = "filter"
private const val IS_SPENDING = "isSpending"

class TransactionCategoryViewModel : BaseConfigCRUDViewModel<TransactionCategoryDao, TransactionCategory>(inject<TransactionCategoryDao>().value)

@Suppress("ParamsComparedByRef")
@Composable
internal fun TransactionCategoryListView(viewModel: TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Transaction Categories",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch {
                    importCsv(inject<TransactionCategoryDao>().value) { toTransactionCategory(it) }
                } },
                ButtonSettings("Export") { coroutineScope.launch {
                    exportToCsv("transactionCategories") { toDataFrame(state.value) }
                }},
                ButtonSettings("+") { it.navigate(TransactionCategory(name = "", filter = false)) })
        }) { paddingValues ->
        LazyVerticalGrid(
            columns = WeightedIconGridCells(2, 2, 1, 1),
            Modifier.padding(paddingValues)
        ) {
            item { ViewText("Name") }
            item { ViewText("Filter") }
            item { ViewText("Is Spending") }
            item(span = { GridItemSpan(2) }) {}
            for (transactionCategory in state.value) {
                item { ViewText(transactionCategory.name) }
                item { ViewText(transactionCategory.filter.toString()) }
                item { ViewText(transactionCategory.isSpending.toString()) }
                item { EditButton { navController -> navController.navigate(transactionCategory) } }
                item { DeleteButton { viewModel.delete(transactionCategory) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditTransactionCategory(transactionCategory: TransactionCategory,
                                     viewModel: TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    var name by remember { mutableStateOf(transactionCategory.name) }
    var filter by remember { mutableStateOf(transactionCategory.filter) }
    var isSpending by remember { mutableStateOf(transactionCategory.isSpending) }
    val title = if (transactionCategory.id == 0) "Add Transaction Category" else "Edit Transaction Category"
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

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
                save(transactionCategory, viewModel, name, filter, isSpending)
                it.popBackStack()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save(transactionCategory, viewModel, name, filter, isSpending)}) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.padding(paddingValues)) {
            gridEntry("Name", name) { name = it
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
        }
    }
}

private fun save(transactionCategory: TransactionCategory?, viewModel: TransactionCategoryViewModel, name: String, filter: Boolean, isSpending: Boolean) {
    if (transactionCategory == null || transactionCategory.id == 0) {
        viewModel.insert(TransactionCategory(name = name, filter = filter, isSpending = isSpending))
    } else {
        viewModel.update(TransactionCategory(transactionCategory.id, name, filter, isSpending))
    }
}

internal fun toDataFrame(transactionCategories: List<TransactionCategory>): DataFrame<TransactionCategory> =
    transactionCategories.toDataFrame {
        NAME from { it.name }
        FILTER from { it.filter }
        IS_SPENDING from { it.isSpending }
    }

internal fun toTransactionCategory(row: DataRow<Any?>): TransactionCategory =
    TransactionCategory(name = row[NAME] as String, filter = row[FILTER] as Boolean, isSpending = row[IS_SPENDING] as Boolean)
