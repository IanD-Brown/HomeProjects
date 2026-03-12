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
import io.github.iandbrown.reconciler.database.TransactionCategory
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.di.inject
import io.github.iandbrown.reconciler.logic.DayDate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import org.koin.compose.koinInject
import kotlin.math.max

private const val BASE_DATE = "01/01/2026"
private var baseDate = DayDate(BASE_DATE).value()

class TransactionViewModel(dao : TransactionDao = inject<TransactionDao>().value) :
    BaseConfigCRUDViewModel<TransactionDao, Transaction>(dao)

@Suppress("ParamsComparedByRef")
@Composable
fun ViewAllTransaction(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                       transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    var minDate by remember {mutableIntStateOf(baseDate)}
    var filterSheet by remember { mutableIntStateOf(0) }
    var filterCategory by remember { mutableIntStateOf(-1) }
    val transactionCategories = transCategoryViewModel.uiState.collectAsState(emptyList())

    ViewCommon(
        "Transactions",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Export") {
                    coroutineScope.launch {
                        export(filterTransaction(state.value, minDate, filterSheet, filterCategory), transactionCategories.value)
                    }
                },
            )
        }) { paddingValues ->
        val categoryNameLookup = transactionCategories.value.associateBy( { it.id }, {it.name} )

        LazyVerticalGrid(columns = WeightedIconGridCells(1, 1, 1, 6, 1, 1), Modifier.padding(paddingValues)) {
            item (span = { GridItemSpan(6) }) {
                ViewText("Filters")
            }
            item {
                DatePickerView(minDate, Modifier.padding(0.dp), { true }) { minDate = it; baseDate = it }
            }
            item {
                DropdownList(listOf("") + Sheet.entries.map { it.displayName() }, 0) {
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
            item { ViewText("Description") }
            item { ViewText("Amount") }
            item(span = { GridItemSpan(2) }) {}
            for (transaction in filterTransaction(state.value, minDate, filterSheet, filterCategory)) {
                item { ViewText(DayDate(transaction.date).toString()) }
                item { ViewText(if (transaction.sheet > 0) Sheet.entries[transaction.sheet - 1].displayName() else "") }
                item { ViewText(transaction.description) }
                item { ViewText(transaction.amount.toString()) }
                item { ViewText(categoryNameLookup[transaction.category] ?: "") }
                item { Icon(
                    Icons.Default.Add,
                    "add rule",
                    Modifier.clickable(onClick =
                        { appNavController.navigate(Rule(0, escapeString(transaction.description), 0))}), Color.Green)}
            }
        }
    }
}

private fun filterTransaction(state: List<Transaction>, minDate: Int, filterSheet: Int, filterCategory: Int): List<Transaction> =
    state
    .filter { it.date >= minDate }
    .filter { filterSheet == 0 || it.sheet == filterSheet }
    .filter { filterCategory == -1 || it.category == filterCategory }
    .sortedBy { it.date }

@Suppress("ParamsComparedByRef")
@Composable
fun ViewSpendingSummary(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                        transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    var minDate by remember {mutableIntStateOf(baseDate)}
    val transactionCategories = transCategoryViewModel.uiState.collectAsState(emptyList())

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

        LazyVerticalGrid(columns = GridCells.Fixed(4), Modifier.padding(paddingValues)) {
            item {ViewText("Filter date ")}
            item {DatePickerView(
                minDate,
                Modifier.padding(0.dp),
                { true }) {
                minDate = it
                baseDate = it
            }}
            item(span = { GridItemSpan(2) }) {}
            item { ViewText("") }
            item { ViewText("Spending") }
            item { ViewText("Credit transactions") }
            item { ViewText("Current transactions") }
            for (month in months) {
                item { ViewText(month.toString().substring(3)) }
                val transactionsForMonth = byMonth[month.value()]
                val total = transactionsForMonth?.sumOf { it.amount }
                formatedNumber("%.2f", total)
                item { ViewText(transactionsForMonth?.filter { it.sheet == Sheet.CREDIT.number }?.size.toString())}
                item { ViewText(transactionsForMonth?.filter { it.sheet == Sheet.CURRENT.number }?.size.toString())}
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

        LazyVerticalGrid(columns = GridCells.Fixed(viewCategories.size + 2), Modifier.padding(paddingValues)) {
            item {ViewText("Filter date ")}
            item {DatePickerView(
                minDate,
                Modifier.padding(0.dp),
                { true }) {
                minDate = it
                baseDate = it
            }}
            item(span = { GridItemSpan(max(1, viewCategories.size)) }) {}
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

internal enum class Sheet(val number : Int) {
    CREDIT(1),
    CURRENT(2);

    fun displayName() = name[0] + name.substring(1).lowercase()
}

private suspend fun export(transactions: List<Transaction>, transactionCategories: List<TransactionCategory>) {
    val file = FileKit.openFileSaver(suggestedName = "transactions", extension = "csv")
    val sink = file?.sink(append = false)?.buffered()

    sink.use { bufferedSink ->
        if (bufferedSink != null) {
            val categoryLookup = transactionCategories.associateBy( { it.id }, {it.name} )
            bufferedSink.writeString("Date,Account,Description,Category,Amount\n")
            for (transaction in transactions) {
                if (transaction.description.indexOf(',') >= 0) {
                    bufferedSink.writeString(
                        "${DayDate(transaction.date)}," +
                                "${Sheet.entries[transaction.sheet - 1].displayName()}," +
                                "\"${transaction.description}\"," +
                                "${categoryLookup[transaction.category]}," +
                                "${transaction.amount}\n")
                } else {
                    bufferedSink.writeString(
                        "${DayDate(transaction.date)}," +
                                "${Sheet.entries[transaction.sheet - 1].displayName()}," +
                                "${transaction.description}," +
                                "${categoryLookup[transaction.category]}," +
                                "${transaction.amount}\n")
                }
            }
        }
    }

}
