package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import io.github.iandbrown.reconciler.database.Transaction
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.di.inject
import io.github.iandbrown.reconciler.logic.DayDate
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.koin.compose.koinInject
import kotlin.math.max

private const val BASE_DATE = "01/01/2026"
private var baseDate = DayDate(BASE_DATE).value()

class TransactionViewModel(dao : TransactionDao = inject<TransactionDao>().value) :
    BaseConfigCRUDViewModel<TransactionDao, Transaction>(dao)

@Suppress("ParamsComparedByRef")
@Composable
fun ViewAllTransaction(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                       transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>(),
                       accountViewModel:AccountViewModel = koinInject<AccountViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    var minDate by remember {mutableIntStateOf(baseDate)}
    var filterSheet by remember { mutableIntStateOf(0) }
    var filterCategory by remember { mutableIntStateOf(-1) }
    val transactionCategories = transCategoryViewModel.uiState.collectAsState(emptyList())
    val accounts = accountViewModel.uiState.collectAsState(emptyList())

    ViewCommon(
        "Transactions",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Export") {
                    coroutineScope.launch {
                        exportToCsv("transactions") {
                            toDataFrame(filterTransaction(state.value, minDate, filterSheet, filterCategory),
                                accounts.value.associateBy( { it.id }, {it.name} ),
                                transactionCategories.value.associateBy( { it.id }, {it.name} ))
                        }
                    }
                },
            )
        }) { paddingValues ->
        val categoryNameLookup = transactionCategories.value.associateBy( { it.id }, {it.name} )
        val accountNameLookup = accounts.value.associateBy( { it.id }, {it.name} )

        LazyVerticalGrid(columns = WeightedIconGridCells(1, 1, 1, 6, 1, 1), Modifier.padding(paddingValues)) {
            item (span = { GridItemSpan(6) }) {
                ViewText("Filters")
            }
            item {
                DatePickerView(minDate, Modifier.padding(0.dp), { true }) { minDate = it; baseDate = it }
            }
            item {
                DropdownList(listOf("") + accounts.value.map { it.name }, 0) {
                    filterSheet = it
                }
            }
            item(span = { GridItemSpan(2) }) {}
            item {
                DropdownList(listOf("") + transactionCategories.value.map { it.name }, 0) {
                    filterCategory = when (it) {
                        0 -> -1
                        else -> transactionCategories.value[it - 1].id
                    }
                }
            }
            item {}
            item(span = { GridItemSpan(2) }) {}
            viewTextItems("Description", "Amount")
            item(span = { GridItemSpan(2) }) {}
            for (transaction in filterTransaction(state.value, minDate, filterSheet, filterCategory)) {
                viewTextItems(
                    DayDate(transaction.date).toString(),
                    accountNameLookup[transaction.account] ?: "",
                    transaction.description,
                    transaction.amount.toString(),
                    categoryNameLookup[transaction.category] ?: "")
                item { Icon(
                    Icons.Default.Add,
                    "add rule",
                    Modifier.clickable(onClick =
                        { appNavController.navigate(Rule(0, escapeString(transaction.description), 0))}), Color.Green)}
            }
        }
    }
}

private fun filterTransaction(state: List<Transaction>, minDate: Int, filterAccount: Int, filterCategory: Int): List<Transaction> =
    state
    .filter { it.date >= minDate }
    .filter { filterAccount == 0 || it.account == filterAccount }
    .filter { filterCategory == -1 || it.category == filterCategory }
    .sortedBy { it.date }

@Suppress("ParamsComparedByRef")
@Composable
fun ViewSpendingSummary(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                        transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>(),
                        accountViewModel:AccountViewModel = koinInject<AccountViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    var minDate by remember {mutableIntStateOf(baseDate)}
    val transactionCategories = transCategoryViewModel.uiState.collectAsState(emptyList())
    val accounts = accountViewModel.uiState.collectAsState(emptyList())

    ViewCommon(
        "Spending Summary",
        bottomBar = {}) { paddingValues ->
        val includeCategories = transactionCategories.value.filter { it.isSpending }.map {it.id}.toSet()
        val displayTransactions = state.value
            .filter { it.date >= minDate }
            .filter { it.category == 0 || includeCategories.contains(it.category) }
        val byMonth = displayTransactions.groupBy { DayDate(it.date).startOfMonth().value() }
        val maxDate = if (displayTransactions.isNotEmpty()) displayTransactions.maxOf { it.date } else minDate
        val months = mutableListOf<DayDate>()
        var date = DayDate(minDate).startOfMonth()
        while (date.value() <= maxDate) {
            months.add(date)
            date = date.nextMonth()
        }

        LazyVerticalGrid(columns = GridCells.Fixed(2 + accounts.value.size), Modifier.padding(paddingValues)) {
            item {ViewText("Filter date ")}
            item {DatePickerView(
                minDate,
                Modifier.padding(0.dp),
                { true }) {
                minDate = it
                baseDate = it
            }}
            if (accounts.value.isNotEmpty()) {
                item(span = { GridItemSpan(accounts.value.size) }) {}
            }
            viewTextItems("Month", "Total")
            for (account in accounts.value) {
                item { ViewText("${account.name} transactions") }
            }
            for (month in months) {
                item { ViewText(month.toString().substring(3)) }
                val transactionsForMonth = byMonth[month.value()]
                val total = transactionsForMonth?.sumOf { it.amount }
                formatedNumber("%.2f", total)
                for (account in accounts.value) {
                    item { ViewText(transactionsForMonth?.filter { it.account == account.id }?.size.toString())}
                }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
fun ViewTransactionSummaryByCategory(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                                     transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    var minDate by remember {mutableIntStateOf(baseDate)}
    val transactionCategories = transCategoryViewModel.uiState.collectAsState(emptyList())

    ViewCommon(
        "Transaction summary by category",
        bottomBar = {}) { paddingValues ->
        val displayTransactions = state.value.filter { it.date >= minDate }
        val summaryByCategory = displayTransactions.groupBy { it.category }
        val maxDate = if (displayTransactions.isNotEmpty()) displayTransactions.maxOf { it.date } else minDate
        val months = mutableListOf<DayDate>()
        var date = DayDate(minDate).startOfMonth()
        while (date.value() <= maxDate) {
            months.add(date)
            date = date.nextMonth()
        }
        val viewCategories = transactionCategories.value.filter { !it.filter }
        val datePickerSpan = if (viewCategories.isEmpty()) 1 else 2

        LazyVerticalGrid(columns = GridCells.Fixed(viewCategories.size + 2), Modifier.padding(paddingValues)) {
            item {ViewText("Filter date ")}
            item(span = { GridItemSpan(datePickerSpan) }) { DatePickerView(
                minDate,
                Modifier.padding(0.dp),
                { true }) {
                minDate = it
                baseDate = it
            }}
            item(span = { GridItemSpan(max(1, viewCategories.size - 1)) }) {}
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
            item {}
            for (category in viewCategories) {
                formatedNumber("%.2f", summaryByCategory[category.id]?.sumOf { it.amount })
            }
            formatedNumber("%.2f", summaryByCategory[null]?.sumOf { it.amount })
        }
    }
}

private fun escapeString(string: String) = string.replace("*", "\\*")

private fun toDataFrame(transactions: List<Transaction>, accountLookup: Map<Int, String>, categoryLookup: Map<Int, String>): DataFrame<Transaction> =
    transactions.toDataFrame {
        "Date" from { DayDate(it.date).toString() }
        "Account" from { accountLookup[it.account] }
        "Description" from { it.description }
        "Category" from { categoryLookup[it.category] }
        "Amount" from { it.amount }
    }
