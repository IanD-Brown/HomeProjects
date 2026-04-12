package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.TransactionListView
import io.github.iandbrown.reconciler.database.TransactionListViewDao
import io.github.iandbrown.reconciler.di.inject
import io.github.iandbrown.reconciler.logic.DayDate
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

private const val BASE_DATE = "01/01/2026"
private var baseDate = DayDate(BASE_DATE).value()

class TransactionListViewModel(dao: TransactionListViewDao = inject<TransactionListViewDao>().value) :
    BaseReadViewModel<TransactionListViewDao, TransactionListView>(dao)

@Suppress("ParamsComparedByRef")
@Composable
fun ViewAllTransaction(viewModel: TransactionListViewModel = koinInject(),
                       transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                       accountViewModel:AccountViewModel = koinInject(),
                       accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var minDate by remember {mutableIntStateOf(baseDate)}
    var filterAccount by remember { mutableIntStateOf(0) }
    var filterCategory by remember { mutableIntStateOf(-1) }
    val transactionCategories = transCategoryViewModel.uiState.collectAsState()
    val accounts = accountViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }

    ViewCommon(
        "Transactions",
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope,"transactions") { output ->
                    toDataFrame(filterTransaction(state.value.values(), minDate, filterAccount, filterCategory, accountGroup))
                        .writeCsv(output)
                }
            )
        },
        states = listOf(state.value, transactionCategories.value, accounts.value, accountGroupState.value)) { paddingValues ->
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
            LazyVerticalGrid(columns = WeightedIconGridCells(1, 1, 1, 6, 1, 1)) {
                item {DatePickerView(minDate, Modifier.padding(0.dp), { true }) {
                    minDate = it
                    baseDate = it
                }}
                item {
                    val value = accounts.value.values()
                    DropdownList(MutableStateFlow(listOf("") + value.map { it.name }), 0) {
                        filterAccount = when (it) {
                            0 -> -1
                            else -> value[it - 1].id
                        }
                    }
                }
                item(span = { GridItemSpan(2) }) {}
                item {
                    val value = transactionCategories.value.values()
                    DropdownList(MutableStateFlow(listOf("") + value.map { it.name }), 0) {
                        filterCategory = when (it) {
                            0 -> -1
                            else -> value[it - 1].id
                        }
                    }
                }
                item {}
                item(span = { GridItemSpan(2) }) {}
                viewTextItems("Description", "Amount")
                item(span = { GridItemSpan(2) }) {}
                for (transaction in filterTransaction(state.value.values(), minDate, filterAccount, filterCategory, accountGroup)) {
                    viewTextItems(
                        DayDate(transaction.date).toString(),
                        transaction.accountName,
                        transaction.description,
                        transaction.amount.toString(),
                        transaction.categoryName
                    )
                    item { Icon(
                            Icons.Default.Add,
                            "add rule",
                            Modifier.clickable(onClick =
                                    { appNavController.navigate(Rule(0, escapeString(transaction.description), 0, transaction.accountGroup)) }), Color.Green
                        )
                    }
                }
            }
        }
    }
}

private fun filterTransaction(state: List<TransactionListView>, minDate: Int, filterAccount: Int, filterCategory: Int, accountGroup: Int): List<TransactionListView> =
    state
        .filter { it.accountGroup == accountGroup }
        .filter { it.date >= minDate }
        .filter { filterAccount == 0 || it.account == filterAccount }
        .filter { filterCategory == -1 || it.category == filterCategory }

@Suppress("ParamsComparedByRef")
@Composable
fun ViewSpendingSummary(viewModel: TransactionListViewModel = koinInject(),
                        transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                        accountViewModel:AccountViewModel = koinInject(),
                        accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    var minDate by remember {mutableIntStateOf(baseDate)}
    val transactionCategories = transCategoryViewModel.uiState.collectAsState()
    val accounts = accountViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }

    ViewCommon(
        "Spending Summary",
        bottomBar = {},
        states = listOf(state.value, transactionCategories.value, accounts.value, accountGroupState.value)) { paddingValues ->
        val includeCategories = transactionCategories.value.values().filter { it.isSpending }.map {it.id}.toSet()
        val displayTransactions = filterTransaction(state.value.values(), minDate, 0, -1, accountGroup)
            .filter { it.category in includeCategories }
        val byMonth = displayTransactions.groupBy { DayDate(it.date).startOfMonth().value() }
        val months = getMonths(displayTransactions, minDate)

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ViewText("Filter date ")
                DatePickerView(minDate, Modifier.padding(0.dp), { true }) {
                    minDate = it
                    baseDate = it
                }
                Spacer(modifier = Modifier.size(16.dp))
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

private fun getMonths(transactions: List<TransactionListView>, minDate: Int): List<DayDate> {
    val months = mutableListOf<DayDate>()
    val maxDate = if (transactions.isNotEmpty()) transactions.maxOf { it.date } else minDate
    var date = DayDate(minDate).startOfMonth()
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
    var minDate by remember {mutableIntStateOf(baseDate)}
    val transactionCategories = transCategoryViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }

    ViewCommon(
        "Transaction summary by category",
        bottomBar = {},
        states = listOf(state.value, transactionCategories.value, accountGroupState.value)) { paddingValues ->
        val displayTransactions = filterTransaction(state.value.values(), minDate, 0, -1, accountGroup)
        val summaryByCategory = displayTransactions.groupBy { it.category }
        val months = getMonths(displayTransactions, minDate)
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
                Spacer(modifier = Modifier.size(16.dp))
                ViewText("Filter date ")
                DatePickerView(minDate, Modifier.padding(0.dp), { true }) {
                    minDate = it
                    baseDate = it
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
                        formatedNumber("%.2f", summaryByCategory[category.id]?.filter { it.date >= month.value() && it.date < nextMonth.value() }?.sumOf { it.amount })
                    }
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

private fun escapeString(string: String) = string.replace("*", "\\*")

private fun toDataFrame(transactions: List<TransactionListView>): DataFrame<TransactionListView> =
    transactions.toDataFrame {
        "Date" from { DayDate(it.date).toString() }
        "Account" from { it.accountName }
        "Description" from { it.description }
        "Category" from { it.categoryName }
        "Amount" from { it.amount }
    }
