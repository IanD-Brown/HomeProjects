package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.window.Dialog
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.Transaction
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.database.TransactionListView
import io.github.iandbrown.reconciler.database.TransactionListViewDao
import io.github.iandbrown.reconciler.di.inject
import io.github.iandbrown.reconciler.logic.DayDate
import io.github.iandbrown.reconciler.logic.TO_STRING_PATTERN
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject
import java.util.Locale

private data class FilterConfig(val minDate: DayDate, val maxDate: DayDate?, val account: Int?, val category: Int?, val matchDistance: Int?)

private var baseFilterConfig = FilterConfig(DayDate.ofCurrentYearStart(), null, null, null, null)

class TransactionListViewModel(dao: TransactionListViewDao = inject<TransactionListViewDao>().value) :
    BaseReadViewModel<TransactionListViewDao, TransactionListView>(dao)

class TransactionViewModel(dao: TransactionDao = inject<TransactionDao>().value) :
    BaseConfigCRUDViewModel<TransactionDao, Transaction>(dao)

@Composable
private fun FilterConfigEditor(fullFilter: Boolean,
                               onDismiss: () -> Unit,
                               onConfirm: (FilterConfig) -> Unit,
                               transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                               accountViewModel:AccountViewModel = koinInject()) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            val transactionCategories = transCategoryViewModel.uiState.collectAsState()
            val accounts = accountViewModel.uiState.collectAsState()
            var minDate by remember { mutableStateOf(baseFilterConfig.minDate) }
            var maxDate by remember { mutableStateOf(baseFilterConfig.maxDate) }
            var account by remember { mutableStateOf(baseFilterConfig.account) }
            var category by remember { mutableStateOf(baseFilterConfig.category) }
            var matchDistance by remember { mutableStateOf(baseFilterConfig.matchDistance) }
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    ViewText("Minimum date")
                    DatePickerView(
                        minDate.value(),
                        Modifier.padding(0.dp),
                        { true }) { minDate = DayDate.of(it) }
                }
                Row {
                    ViewText("Maximum date")
                    DatePickerView(maxDate?.value() ?: 0, Modifier.padding(0.dp), { true }) {
                            maxDate = if (it > 0) DayDate.of(it) else null
                        }
                }
                if (fullFilter) {
                    Row {
                        ViewText("Account")
                        when (accounts.value) {
                            is ViewModelState.Success -> {
                                val accountValues = accounts.value.values()
                                DropdownList(
                                    MutableStateFlow(listOf("") + accountValues.map { it.name }),
                                    if (account == null) 0 else accountValues.map { it.id }.indexOf(account)
                                ) {
                                    account = when (it) {
                                        0 -> null
                                        else -> accountValues[it - 1].id
                                    }
                                }
                            }
                            else -> {
                                ViewText("")
                            }
                        }
                    }
                    Row {
                        ViewText("Transaction category")
                        when (transactionCategories.value) {
                            is ViewModelState.Success -> {
                                val transactionCategoryValues = transactionCategories.value.values()
                                DropdownList(
                                    MutableStateFlow(listOf("") + transactionCategoryValues.map { it.name }),
                                    if (category == null) 0 else transactionCategoryValues.map { it.id }.indexOf(category)
                                ) {
                                    category = when (it) {
                                        0 -> null
                                        else -> transactionCategoryValues[it - 1].id
                                    }
                                }
                            }
                            else -> {ViewText("")}
                        }
                    }
                    Row {
                        ViewText("Match distance")
                        ViewTextField(matchDistance?.toString() ?: "0") {
                            val i = it.toIntOrNull()
                            if (i != null) {
                                matchDistance = if (i > 0) i else null
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(FilterConfig(minDate, maxDate, account, category, matchDistance)) }) { Text("OK") }
                }
            }
        }
    }

}

@Suppress("ParamsComparedByRef")
@Composable
fun ViewAllTransaction(viewModel: TransactionListViewModel = koinInject(),
                       transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                       accountViewModel:AccountViewModel = koinInject(),
                       accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val transactionCategories = transCategoryViewModel.uiState.collectAsState()
    val accounts = accountViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }
    var filterConfig by remember { mutableStateOf(baseFilterConfig)}

    if (showDialog) {
        FilterConfigEditor(true, { showDialog = false}, {
                baseFilterConfig = it
                filterConfig = it
                showDialog = false})
    } else {
        ViewCommon(
            "Transactions",
            bottomBar = {
                BottomBarWithButtons(
                    exportButtonSettings(coroutineScope, "transactions") { output ->
                        toDataFrame(filterTransaction(state.value.values(), true, accountGroup))
                            .writeCsv(output)
                    }
                )
            },
            states = listOf(state.value, transactionCategories.value, accounts.value, accountGroupState.value),
            actions = {
                IconButton({ showDialog = true }) { Icon(Icons.Default.Menu, "") }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ViewText("Account Group")
                    Spacer(modifier = Modifier.size(16.dp))
                    val value = accountGroupState.value.values()
                    DropdownList(
                        MutableStateFlow(value.map { it.name }),
                        value.map { it.id }.indexOf(accountGroup)
                    ) {
                        accountGroup = value[it].id
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    ViewText("Filters")
                }
                LazyVerticalGrid(columns = WeightedIconGridCells(2, 1, 1, 6, 1, 1)) {
                   item(span = { GridItemSpan(2) }) {}
                    viewTextItems("Description", "Amount")
                    item(span = { GridItemSpan(3) }) {}
                    for (transaction in filterTransaction(state.value.values(), true, accountGroup)) {
                        viewTextItems(
                            DayDate.of(transaction.date).toString(),
                            transaction.accountName,
                            transaction.description,
                            transaction.amount.toString(),
                            transaction.categoryName
                        )
                        item { EditButton { navController -> navController.navigate(transaction) } }
                        item {
                            Icon(
                                Icons.Default.Add,
                                "add rule",
                                Modifier.clickable(
                                    onClick =
                                        { appNavController.navigate(Rule(0, escapeString(transaction.description), 0, transaction.accountGroup)) }), Color.Green
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun filterTransaction(state: List<TransactionListView>,
                              fullFilter: Boolean,
                              accountGroup: Int): List<TransactionListView> {
    val filtered = state
        .asSequence()
        .filter { it.accountGroup == accountGroup }
        .filter { it.date >= baseFilterConfig.minDate.value()  && (baseFilterConfig.maxDate == null || it.date <= baseFilterConfig.maxDate!!.value())}
        .filter { !fullFilter || baseFilterConfig.account == null || it.account == baseFilterConfig.account }
        .filter { !fullFilter || baseFilterConfig.category == null || it.category == baseFilterConfig.category }
        .toMutableList()

    if (fullFilter && (baseFilterConfig.matchDistance ?: 0) > 0) {
        val removed = mutableSetOf<TransactionListView>()
        for (i in 0..filtered.lastIndex) {
            if (removed.contains(filtered[i])) {
                continue
            }
            for (j in (i+1)..(filtered.lastIndex.fastCoerceAtMost(i + baseFilterConfig.matchDistance!!))) {
                if (removed.contains(filtered[j])) {
                    continue
                }
                val added = filtered[i].amount + filtered[j].amount
                if (added >= -0.1 && added <= 0.1) {
                    removed.add(filtered[i])
                    removed.add(filtered[j])
                    break
                }
            }
        }
        filtered.removeAll(removed)
        println("Removed ${removed.size}")
        removed.forEach { println("${it.account}, ${DayDate.of(it.date)}, ${it.description}, ${it.amount}") }
    }

    return filtered
}

@Composable
internal fun EditTransaction(item: TransactionListView) {
    var description by remember { mutableStateOf(item.description) }
    var amount by remember { mutableDoubleStateOf(item.amount) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }
    var split by remember { mutableStateOf<Transaction?>(null)}

    fun setEditorState() {
        editorState = if (description == item.description && amount == item.amount && split == null) {
            EditorState.CLEAN
        } else if (description.isEmpty() || (split != null && split!!.description.isEmpty()) || (split != null && amount + split!!.amount != item.amount)) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    ViewCommon(
        "Edit transaction",
        description = "Return to Transactions",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Split", split == null) {
                    split = Transaction(account = item.account, date = item.date, description = description, amount = 0.0)
                    setEditorState()
                },
                ButtonSettings(enabled = editorState == EditorState.VALID) {
                    save(Transaction(item.id, item.account, item.date, description, amount, item.category), split)
                    it.popBackStack()
                }
            )
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save(Transaction(item.id, item.account, item.date, description, amount, item.category), split)},
        states = listOf()) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(5), Modifier.padding(paddingValues)) {
            viewTextItems("Date", "Account", "Description", "Amount", "Category")
            item { ViewText(DayDate.of(item.date).toString())}
            item { ViewText(item.accountName)}
            item { ViewTextField(description) {
                description = it.trim()
                setEditorState()
            }}
            item { ViewTextField(String.format(Locale.UK, "%.2f", amount), onValueChange = { newValue ->
                // Filter to allow only digits and one decimal point
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                // Ensure only one decimal point exists
                if (filtered.count { it == '.' } <= 1) {
                    amount = newValue.toDouble()
                    setEditorState()
                }
            })}
            item { ViewText(item.categoryName) }
            if (split != null) {
                item { ViewText(DayDate.of(item.date).toString())}
                item { ViewText(item.accountName)}
                item { ViewTextField(split!!.description) {
                    split = Transaction(account = item.account, date = item.date, description = it.trim(), amount = split!!.amount)
                    setEditorState()
                }}
                item { ViewTextField(String.format(Locale.UK, "%.2f", split!!.amount), onValueChange = { newValue ->
                    // Filter to allow only digits and one decimal point
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    // Ensure only one decimal point exists
                    if (filtered.count { it == '.' } <= 1) {
                        split = Transaction(account = item.account, date = item.date, description = split!!.description, amount = newValue.toDouble())
                        setEditorState()
                    }
                })}
                item {  }
            }
        }
    }
}

private fun save(transaction: Transaction, split: Transaction?, viewModel : TransactionViewModel = inject<TransactionViewModel>().value) {
    viewModel.update(transaction)
    if (split != null) {
        viewModel.insert(Transaction(account = transaction.account, date = transaction.date, description = split.description, amount = split.amount))
    }
}

@Suppress("ParamsComparedByRef")
@Composable
fun ViewSpendingSummary(viewModel: TransactionListViewModel = koinInject(),
                        transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                        accountViewModel:AccountViewModel = koinInject(),
                        accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val transactionCategories = transCategoryViewModel.uiState.collectAsState()
    val accounts = accountViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }
    var filterConfig by remember { mutableStateOf(baseFilterConfig)}

    if (showDialog) {
        FilterConfigEditor(false, { showDialog = false}, {
                baseFilterConfig = it
                filterConfig = it
                showDialog = false})
    } else {
        ViewCommon(
            "Spending Summary",
            bottomBar = {},
            states = listOf(state.value, transactionCategories.value, accounts.value, accountGroupState.value),
            actions = {
                IconButton({ showDialog = true }) { Icon(Icons.Default.Menu, "") }
            }
        ) { paddingValues ->
            val includeCategories =
                transactionCategories.value.values().filter { it.isSpending }.map { it.id }.toSet()
            val displayTransactions = filterTransaction(state.value.values(), false, accountGroup)
                .filter { it.category in includeCategories }
            val byMonth = displayTransactions.groupBy { DayDate.of(it.date).startOfMonth().value() }
            val months = getMonths(displayTransactions, filterConfig.minDate)

            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ViewText("Account Group")
                    Spacer(modifier = Modifier.size(16.dp))
                    val value = accountGroupState.value.values()
                    DropdownList(
                        MutableStateFlow(value.map { it.name }),
                        value.map { it.id }.indexOf(accountGroup)
                    ) {
                        accountGroup = value[it].id
                    }
                }

                val data = accounts.value.values()
                LazyVerticalGrid(columns = GridCells.Fixed(data.size + 2)) {
                    viewTextItems("Month", "Total")
                    for (account in data) {
                        item { ViewText("${account.name} transactions") }
                    }
                    for (month in months) {
                        item { ViewText(month.toString().substring(3)) }
                        val transactionsForMonth = byMonth[month.value()]
                        val total = transactionsForMonth?.sumOf { it.amount }
                        formatedNumber("%.2f", total)
                        for (account in data) {
                            item { ViewText(transactionsForMonth?.filter { it.account == account.id }?.size.toString()) }
                        }
                    }
                }
            }
        }
    }
}

private fun getMonths(transactions: List<TransactionListView>, minDate: DayDate): List<DayDate> {
    val months = mutableListOf<DayDate>()
    val maxDate = if (transactions.isNotEmpty()) transactions.maxOf { it.date } else minDate.value()
    var date = minDate.startOfMonth()
    while (date.value() <= maxDate) {
        months.add(date)
        date = date.nextMonth()
    }
    return months
}

@Suppress("ParamsComparedByRef")
@Composable
fun ViewTransactionSummaryByCategory(viewModel: TransactionListViewModel = koinInject(),
                                     transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                                     accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val transactionCategories = transCategoryViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }
    var filterConfig by remember { mutableStateOf(baseFilterConfig)}

    if (showDialog) {
        FilterConfigEditor(false, { showDialog = false}, {
                baseFilterConfig = it
                filterConfig = it
                showDialog = false})
    } else {
        ViewCommon(
            "Transaction summary by category",
            bottomBar = {},
            states = listOf(state.value, transactionCategories.value, accountGroupState.value),
            actions = {
                IconButton({ showDialog = true }) { Icon(Icons.Default.Menu, "") }
            }
        ) { paddingValues ->
            val displayTransactions = filterTransaction(state.value.values(), false, accountGroup)
            val summaryByCategory = displayTransactions.groupBy { it.category }
            val months = getMonths(displayTransactions, filterConfig.minDate)
            val viewCategories = transactionCategories.value.values().filter { !it.filter }

            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ViewText("Account Group")
                    Spacer(modifier = Modifier.size(16.dp))
                    val value = accountGroupState.value.values()
                    DropdownList(
                        MutableStateFlow(value.map { it.name }),
                        value.map { it.id }.indexOf(accountGroup)
                    ) {
                        accountGroup = value[it].id
                    }
                }

                LazyVerticalGrid(columns = GridCells.Fixed(viewCategories.size + 2)) {
                    // Second row - headers, blank for month
                    item { ViewText("") }
                    for (category in viewCategories) {
                        item { ViewText(category.name) }
                    }
                    item { ViewText("") }
                    for (month in months) {
                        item { ViewText(month.toString().substring(3)) }
                        val nextMonth = month.nextMonth()
                        for (category in viewCategories) {
                            formatedNumber("%.2f", summaryByCategory[category.id]?.filter { it.date >= month.value() && it.date < nextMonth.value() }?.sumOf { it.amount }) }
                        formatedNumber("%.2f", summaryByCategory[null]?.filter { it.date >= month.value() && it.date < nextMonth.value() }?.sumOf { it.amount })
                    }
                    item {} // No month for totals
                    for (category in viewCategories) {
                        formatedNumber("%.2f", summaryByCategory[category.id]?.sumOf { it.amount })
                    }
                    formatedNumber("%.2f", summaryByCategory[null]?.sumOf { it.amount })
                }
            }
        }
    }
}

private fun escapeString(string: String) = string.replace("*", "\\*")

internal fun toDataFrame(transactions: List<TransactionListView>): DataFrame<TransactionListView> =
    transactions.toDataFrame {
        "Date" from { DayDate.of(it.date).toString() }
        "Account" from { it.accountName }
        "Description" from { it.description }
        "Category" from { it.categoryName }
        "Amount" from { it.amount }
    }


internal suspend fun toTransaction(row: DataRow<Any?>, accountDao: AccountDao = inject<AccountDao>().value): Transaction =
    Transaction(
        account = accountDao.getByName(row["Account"] as String)!!,
        date = DayDate.of(row["Date"] as String, TO_STRING_PATTERN).value(),
        description = row["Description"] as String,
        amount = (row["Amount"] as Float).toDouble()
    )
