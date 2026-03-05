package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.koin.compose.koinInject
import kotlin.math.abs

class TransactionViewModel(dao : TransactionDao = inject<TransactionDao>().value) :
    BaseConfigCRUDViewModel<TransactionDao, Transaction>(dao)

@Composable
fun NavigateTransaction(argument: String?) {
    when (argument) {
        "View" -> TransactionEditor()
    }
}

@Composable
fun NavigateTransactionByAmount(argument: String?) {
    when (argument) {
        "View" -> TransactionByGroup()
    }
}

@Composable
private fun TransactionEditor(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                              ruleModel: RuleViewModel = koinInject<RuleViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    val ruleState = ruleModel.uiState.collectAsState(emptyList())
    var minDate by remember {mutableIntStateOf(DayDate("01/01/2026").value())}

    ViewCommon(
        "Transactions",
        bottomBar = {BottomBarWithButton("load") {
            coroutineScope.launch {
                load()
            }
        }}) { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(1, 1, 1, 6, 1), Modifier.padding(paddingValues)) {
                item {ViewText("Filter date ")}
                item {}
                item {DatePickerView(
                    minDate,
                    Modifier.padding(0.dp),
                    { true }) {
                    minDate = it
                }}
                item(span = { GridItemSpan(2) }) {}
                item { ViewText("Date") }
                item {}
                item { ViewText("Description") }
                item { ViewText("Amount") }
                item {}
                for (transaction in filterTransactions(minDate, state.value, ruleState.value).sortedBy { it.date }) {
                    item { ViewText(DayDate(transaction.date).toString()) }
                    item { ViewText(if (transaction.sheet > 0) Sheet.entries[transaction.sheet - 1].displayName() else "") }
                    item { ViewText(transaction.description) }
                    item { ViewText(transaction.amount.toString()) }
                    item { Icon(
                        Icons.Default.Add,
                        "add rule",
                        Modifier.clickable(onClick =
                            { appNavController.navigate(Rule(0, transaction.description, 0))}), Color.Green)}
                }
            }
        }
}

@Composable
private fun TransactionByGroup(viewModel: TransactionViewModel = koinInject<TransactionViewModel>(),
                              ruleModel: RuleViewModel = koinInject<RuleViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val ruleState = ruleModel.uiState.collectAsState(emptyList())
    var minDate by remember {mutableIntStateOf(DayDate("01/01/2026").value())}

    ViewCommon(
        "Transactions by amount",
        bottomBar = {}) { paddingValues ->
        LazyVerticalGrid(columns = WeightedIconGridCells(0, 1, 1, 6, 1), Modifier.padding(paddingValues)) {
            item {ViewText("Filter date ")}
            item {}
            item {DatePickerView(
                minDate,
                Modifier.padding(0.dp),
                { true }) {
                minDate = it
            }}
            item {}
            item { ViewText("Date") }
            item {}
            item { ViewText("Description") }
            item { ViewText("Amount") }
            val transactions = transactionsByAmount(ruleState.value, state.value, minDate)

            for (amount in transactions.keys.sortedBy { it }) {
                for (transaction in transactions[amount]!!) {
                    item { ViewText(DayDate(transaction.date).toString()) }
                    item { ViewText(if (transaction.sheet > 0) Sheet.entries[transaction.sheet - 1].displayName() else "") }
                    item { ViewText(transaction.description) }
                    item { ViewText(transaction.amount.toString()) }
                }
            }
        }
    }
}

private fun filterTransactions(minDate: Int, all: List<Transaction>, rules: List<Rule>): List<Transaction> {
    val finalFilter = rules.filter { it.type == RuleType.OTHER.ordinal }.map { it.match.toRegex() }
    return transactionsByAmount(rules, all, minDate)
        .values
        .flatten()
        .filter {transaction -> finalFilter.none { it.containsMatchIn(transaction.description) } }
}

private fun transactionsByAmount(rules: List<Rule>, all: List<Transaction>, minDate: Int): Map<Double, List<Transaction>> {
    val initialFilter = rules
        .filter { it.type == RuleType.NOISE.ordinal || it.type == RuleType.INCOME.ordinal }
        .map { it.match.toRegex() }
    val reduced = all
        .filter { it.amount > 0.0 }
        .filter { it.date >= minDate }
        .filter { transaction -> initialFilter.none { it.containsMatchIn(transaction.description) } }
        .groupBy { abs(it.amount) }
    return reduced.filter { entry -> entry.value.sumOf { it.amount } != 0.0 }
}

private enum class Sheet(val sheetName: String, val columns: String, val number : Int) {
    CREDIT("Credit card", "B,F,H,J", 1),
    CURRENT("Current account", "B,D,F,G", 2);

    fun displayName() = name[0] + name.substring(1).lowercase()
}

private suspend fun load(transactionDao: TransactionDao = inject<TransactionDao>().value) {
    val spreadSheetFile = FileKit.openFilePicker(FileKitType.File(listOf("xlsx", "xls")), mode = FileKitMode.Single)
    if (spreadSheetFile != null && spreadSheetFile.exists()) {
         transactionDao.deleteAll()

        for (sheet in Sheet.entries) {
            val df = DataFrame.readExcel(spreadSheetFile.toString(), sheet.sheetName, columns = sheet.columns)
            var rowNumber = 0
            for (row in df) {
                val cell0 = row[0]
                if (cell0 is LocalDateTime) {
                    val date = DayDate(cell0)
                    val amount = asDouble(row[2]) - asDouble(row[3])
                    transactionDao.insert(Transaction(sheet.number, rowNumber++, date.value(), description(row[1]), amount))
                }
            }
        }
    }
}

private fun asDouble(value: Any?) = value as? Double ?: 0.0

private fun description(value: Any?) = value as? String ?: "Unknown"
