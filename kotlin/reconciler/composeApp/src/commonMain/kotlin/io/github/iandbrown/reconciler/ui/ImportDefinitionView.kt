package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.database.AccountImportDefinition
import io.github.iandbrown.reconciler.database.AccountImportDefinitionDao
import io.github.iandbrown.reconciler.database.ImportDefinition
import io.github.iandbrown.reconciler.database.ImportDefinitionDao
import io.github.iandbrown.reconciler.database.ImportDefinitionListView
import io.github.iandbrown.reconciler.database.ImportDefinitionListViewDao
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.Transaction
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.di.inject
import io.github.iandbrown.reconciler.logic.DayDate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.JoinType
import org.jetbrains.kotlinx.dataframe.api.join
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

private const val ACCOUNT = "Account"
private const val ACCOUNT_NAME = "AccountName"
private const val ACTIVE = "Active"
private const val AMOUNT_IN = "AmountIn"
private const val AMOUNT_OUT = "AmountOut"
private const val CLEAR = "Clear"
private const val DATE_COLUMN = "DateColumn"
private const val DEFINITION = "Definition"
private const val DEFINITION_NAME = "DefinitionName"
private const val DESCRIPTION_COLUMN = "DescriptionColumn"
private const val SHEET_NAME = "SheetName"
private const val TYPE = "Type"

class ImportDefinitionViewModel : BaseConfigCRUDViewModel<ImportDefinitionDao, ImportDefinition>(inject<ImportDefinitionDao>().value) {
    suspend fun save(importId: Int, name: String, importDefinitions: (Int) -> List<AccountImportDefinition>) : Boolean {
        try {
            dao.save(importId, name, importDefinitions)
            return true
        }  catch (e: Exception) {
            handleException(e)
        }
        return false
    }
}

class ImportDefinitionListViewModel : BaseReadViewModel<ImportDefinitionListViewDao, ImportDefinitionListView>(inject<ImportDefinitionListViewDao>().value) {
    fun delete(item: ImportDefinitionListView, importDefinitionDao: ImportDefinitionDao = inject<ImportDefinitionDao>().value) {
        viewModelScope.launch {
            importDefinitionDao.deleteById(item.importDefinitionId)
        }
    }

    suspend fun monitorImport() = import({handleException(it)})

    suspend fun performImport(importDefinitions: List<ImportDefinitionListView>) =
        perform({ handleException(it) }, importDefinitions)

}

@Suppress("ParamsComparedByRef")
@Composable
fun ImportDefinitionList(viewModel: ImportDefinitionListViewModel = koinInject<ImportDefinitionListViewModel>(),
                         accountViewModel: AccountViewModel = koinInject<AccountViewModel>()) {
    val state = viewModel.uiState.collectAsState()
    val accountState = accountViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Import Definitions",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch { viewModel.monitorImport() }},
                ButtonSettings("Export") { coroutineScope.launch {
                    exportToFile("importDefinitions") { toDataFrame(state.value.values()).writeCsv(it) }
                }},
                ButtonSettings("+") { it.navigate(ImportDefinitionListView()) })
        },
        states = listOf(state.value, accountState.value)) { paddingValues ->
        var importDefinitionId = 0

        LazyVerticalGrid(
            columns = WeightedIconGridCells(3, 5, 1, 5, 1, 1, 1, 1),
            Modifier.padding(paddingValues)
        ) {
            val accountValues = state.value.values().associateBy { Pair(it.importDefinitionId, it.accountId) }
            val orderedAccounts = accountState.value.values().sortedBy { it.name }
            for (item in state.value.values()) {
                if (item.importDefinitionId != importDefinitionId) {
                    importDefinitionId = item.importDefinitionId
                    item(span = { GridItemSpan(7) }) { ViewText(item.name) }
                    item {
                        Icon(
                            Icons.Default.PlayArrow,
                            "run",
                            Modifier.clickable(onClick = {
                                coroutineScope.launch { viewModel.performImport(state.value.values()
                                    .filter { it.importDefinitionId == item.importDefinitionId && it.active }) }
                            }),
                            Color.Green
                        )
                    }
                    item { EditButton { navController -> navController.navigate(item) } }
                    item { DeleteButton { viewModel.delete(item) } }
                    for (account in orderedAccounts) {
                        val item = accountValues[Pair(item.importDefinitionId, account.id)]
                        item { ViewText("  * ${account.name}") }
                        if (item != null) {
                            item { Checkbox(item.active, {}, enabled = false) }
                            viewTextItems(item.sheetName, item.dateColumn, item.descriptionColumn, item.amountInColumn, item.amountOutColumn)
                            item(span = { GridItemSpan(3) }) { }
                        } else {
                            item { Checkbox(false, {}, enabled = false) }
                            item(span = { GridItemSpan(8) }) { }
                        }
                    }
                }
             }
        }
    }
}

private enum class StringField{SHEET_NAME_COLUMN, DATE_COLUMN, DESCRIPTION_COLUMN, AMOUNT_IN_COLUMN, AMOUNT_OUT_COLUMN}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditImportDefinition(importDefinitionListView: ImportDefinitionListView,
                                  viewModel: ImportDefinitionListViewModel = koinInject<ImportDefinitionListViewModel>(),
                                  accountViewModel: AccountViewModel = koinInject<AccountViewModel>(),
                                  importDefinitionViewModel: ImportDefinitionViewModel = koinInject<ImportDefinitionViewModel>()) {
    val definitionState = viewModel.uiState.collectAsState()
    val accountState = accountViewModel.uiState.collectAsState()
    val title = if (importDefinitionListView.importDefinitionId == 0) "Add Import Definition" else "Edit Import Definition"
    var name by remember { mutableStateOf(importDefinitionListView.name) }
    val accounts = accountState.value.values().associateBy { it.id }.toMutableMap()
    val activeEdits = remember { mutableStateMapOf<Int, Boolean>() }
    val clearEdits = remember { mutableStateMapOf<Int, Boolean>() }
    val sheetNameEdits = remember { mutableStateMapOf<Int, String>() }
    val descriptionEdits = remember { mutableStateMapOf<Int, String>() }
    val dateEdits = remember { mutableStateMapOf<Int, String>() }
    val amountInEdits = remember { mutableStateMapOf<Int, String>() }
    val amountOutEdits = remember { mutableStateMapOf<Int, String>() }
    var valid by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val importDefinitionState = importDefinitionViewModel.uiState.collectAsState()

    fun toImportDefinitions(importDefinitionId: Int): List<AccountImportDefinition> {
        val editingDefinitions = definitionState.value.values()
            .filter { it.importDefinitionId == importDefinitionListView.importDefinitionId }.associateBy { it.accountId }

        return accounts.values.map {
            val def = editingDefinitions[it.id] ?: ImportDefinitionListView(importDefinitionId = 0, accountId = it.id)

            AccountImportDefinition(
                it.id,
                importDefinitionId,
                if (activeEdits.containsKey(it.id)) activeEdits[it.id]!! else def.active,
                if (clearEdits.containsKey(it.id)) clearEdits[it.id]!! else def.clear,
                if (sheetNameEdits.containsKey(it.id)) sheetNameEdits[it.id]!! else def.sheetName,
                if (dateEdits.containsKey(it.id)) dateEdits[it.id]!! else def.dateColumn,
                if (descriptionEdits.containsKey(it.id)) descriptionEdits[it.id]!! else def.descriptionColumn,
                if (amountInEdits.containsKey(it.id)) amountInEdits[it.id]!! else def.amountInColumn,
                if (amountOutEdits.containsKey(it.id)) amountOutEdits[it.id]!! else def.amountOutColumn
            )
        }
    }

    fun setValid() {
        valid = name.isNotEmpty() && toImportDefinitions(0)
            .none { it.active && (it.dateColumn.isEmpty() || it.descriptionColumn.isEmpty() || it.amountInColumn.isEmpty() || it.amountOutColumn.isEmpty()) }
    }

    fun LazyGridScope.gridEntry(edits: MutableMap<Int, Boolean>, listView: ImportDefinitionListView, activeField: Boolean) {
        item {
            Checkbox(checked =
                (if (edits.containsKey(listView.accountId)) {
                    edits[listView.accountId]!!
                } else if (activeField) {
                    listView.active
                }else {
                    listView.clear
                }),
                onCheckedChange = {
                    edits[listView.accountId] = it
                    setValid()
                })
            }
    }

    fun LazyGridScope.gridEntry(edits: MutableMap<Int, String>, listView: ImportDefinitionListView, field: StringField) {
        item {
            ViewTextField(value =
                (if (edits.containsKey(listView.accountId)) {
                    edits[listView.accountId]!!
                } else {
                    when (field) {
                        StringField.SHEET_NAME_COLUMN -> listView.sheetName
                        StringField.DATE_COLUMN -> listView.dateColumn
                        StringField.DESCRIPTION_COLUMN -> listView.descriptionColumn
                        StringField.AMOUNT_IN_COLUMN -> listView.amountInColumn
                        StringField.AMOUNT_OUT_COLUMN -> listView.amountOutColumn
                    }
                }),
                onValueChange = {
                    edits[listView.accountId] = it
                    setValid()
                })
        }
    }

    fun hasEdit() : Boolean =
        importDefinitionState.value !is ViewModelState.Error && (name != importDefinitionListView.name ||
                listOf(activeEdits, clearEdits, sheetNameEdits, descriptionEdits, dateEdits, amountInEdits, amountOutEdits).none {it.isNotEmpty()})

    ViewCommon(
        title,
        description = "Return to Import Definitions",
        bottomBar = {
            if (importDefinitionState.value !is ViewModelState.Error) {
                BottomBarWithButton(enabled = valid) { navController ->
                    coroutineScope.launch {
                        if (importDefinitionViewModel.save(importDefinitionListView.importDefinitionId, name)
                        { toImportDefinitions(it) }) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        },
        confirm = {valid && hasEdit()},
        confirmAction = {
            coroutineScope.launch {
                importDefinitionViewModel.save(importDefinitionListView.importDefinitionId, name) {
                    toImportDefinitions(it)
                }
            }
        },
        states = listOf(definitionState.value, accountState.value, importDefinitionState.value),
    ) { paddingValues ->
        val editingDefinitions = definitionState.value.values()
            .filter { it.importDefinitionId == importDefinitionListView.importDefinitionId }
            .associateBy { it.accountId }
        LazyVerticalGrid(columns = GridCells.Fixed(8), Modifier.padding(paddingValues)) {
            viewTextItems("Name", "Active", "Clear", "Sheet Name", "Date Column", "Description Column", "Amount In Column", "Amount Out Column")
            item { ViewTextField(name, onValueChange = { name = it }) }
            item(span = { GridItemSpan(7) }) { }
            for (account in accounts.values.sortedBy { it.name }) {
                val def = editingDefinitions[account.id] ?: ImportDefinitionListView(importDefinitionId = 0, accountId = account.id)
                item { ViewText(account.name) }
                gridEntry(activeEdits, def, true)
                gridEntry(clearEdits, def, false)
                gridEntry(sheetNameEdits, def, StringField.SHEET_NAME_COLUMN)
                gridEntry(dateEdits, def, StringField.DATE_COLUMN)
                gridEntry(descriptionEdits, def, StringField.DESCRIPTION_COLUMN)
                gridEntry(amountInEdits, def, StringField.AMOUNT_IN_COLUMN)
                gridEntry(amountOutEdits, def, StringField.AMOUNT_OUT_COLUMN)
            }
        }
    }
}

private suspend fun perform(exceptionHandler: (Exception) -> Unit,
    importDefinitions: List<ImportDefinitionListView>,
    transactionDao: TransactionDao = inject<TransactionDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value
) {
    tryTransaction(exceptionHandler, {
        val spreadSheetFile = FileKit.openFilePicker(FileKitType.File(listOf("xlsx", "xls")), mode = FileKitMode.Single)
        if (spreadSheetFile != null && spreadSheetFile.exists()) {
            val ruleCategoryMap = ruleDao.getRules().groupBy({ it.match.toRegex() }, { it.category })
            for (importDefinition in importDefinitions) {
                if (importDefinition.clear) {
                    transactionDao.deleteAllByAccount(importDefinition.accountId)
                }
                perform(spreadSheetFile, importDefinition.sheetName, columns(importDefinition), ruleCategoryMap, transactionDao, importDefinition.accountId)
            }
        }
    })
}

private fun columns(def: ImportDefinitionListView) =
    "${def.dateColumn},${def.descriptionColumn},${def.amountInColumn},${def.amountOutColumn}"

private suspend fun perform(
    spreadSheetFile: PlatformFile,
    sheetName: String,
    columns: String,
    ruleCategoryMap: Map<Regex, List<Int>>,
    transactionDao: TransactionDao,
    accountId: Int) {
    val df = DataFrame.readExcel(spreadSheetFile.readBytes().inputStream(), sheetName, columns = columns)
    for (row in df) {
        val cell0 = row[0]
        if (cell0 is LocalDateTime) {
            val date = DayDate(cell0)
            val amount = asDouble(row[2]) - asDouble(row[3])
            if (amount != 0.0) {
                val description = description(row[1])
                val category =
                    ruleCategoryMap.entries.firstOrNull { it.key.containsMatchIn(description) }?.value?.firstOrNull()
                transactionDao.insert(
                    Transaction(account = accountId, date = date.value(), description = description, amount = amount, category = category)
                )
            }
        }
    }
}

internal fun asDouble(value: Any?) = value as? Double ?: 0.0

internal fun description(value: Any?) = value as? String ?: "Unknown"

internal fun toDataFrame(importDefinitions: List<ImportDefinitionListView>): DataFrame<ImportDefinitionListView> =
    importDefinitions.groupBy { it.importDefinitionId }.map { it.value.first() }
        .toDataFrame {
            TYPE from { DEFINITION }
            DEFINITION_NAME from { it.name }
        }.join(importDefinitions.toDataFrame {
            TYPE from { ACCOUNT }
            DEFINITION_NAME from { it.name }
            ACCOUNT_NAME from { it.accountName }
            ACTIVE from { it.active }
            CLEAR from { it.clear }
            SHEET_NAME from { it.sheetName }
            DATE_COLUMN from { it.dateColumn }
            DESCRIPTION_COLUMN from { it.descriptionColumn }
            AMOUNT_IN from { it.amountInColumn }
            AMOUNT_OUT from { it.amountOutColumn }
        }, JoinType.Full)

private suspend fun import(exceptionHandler: (Exception) -> Unit,
                           dao: ImportDefinitionDao = inject<ImportDefinitionDao>().value,
                           accountImportDefinitionDao: AccountImportDefinitionDao = inject<AccountImportDefinitionDao>().value,
                           accountDao: AccountDao = inject<AccountDao>().value) {
    tryTransaction(exceptionHandler, {
        val dataFile =
            FileKit.openFilePicker(FileKitType.File(listOf("csv")), mode = FileKitMode.Single)
        if (dataFile != null && dataFile.exists()) {
            dao.deleteAll()

            DataFrame.readCsv(dataFile.readBytes().inputStream())
                .rows().forEach { importRow(it, dao, accountImportDefinitionDao, accountDao) }
        }
    })
}

internal suspend fun importRow(row: DataRow<Any?>,
                               dao: ImportDefinitionDao = inject<ImportDefinitionDao>().value,
                               accountImportDefinitionDao: AccountImportDefinitionDao = inject<AccountImportDefinitionDao>().value,
                               accountDao: AccountDao = inject<AccountDao>().value) {
    when (row[TYPE]) {
        DEFINITION -> dao.insert(ImportDefinition(name = string(row[DEFINITION_NAME])))
        ACCOUNT -> accountImportDefinitionDao.insert(AccountImportDefinition(
                accountDao.getByName(string(row[ACCOUNT_NAME]))!!,
            dao.getByName(string(row[DEFINITION_NAME]))!!,
            boolean(row[ACTIVE]),
            boolean(row[CLEAR]),
            string(row[SHEET_NAME]),
            string(row[DATE_COLUMN]),
            string(row[DESCRIPTION_COLUMN]),
            string(row[AMOUNT_IN]),
            string(row[AMOUNT_OUT])))
    }
}

private fun boolean(cell: Any?) : Boolean =
    when (cell) {
        is Boolean -> cell
        is String -> cell.toBoolean()
        else -> false
    }

private fun string(cell: Any?): String =
    when (cell) {
        is String -> cell
        is Double -> cell.toString()
        is Char -> cell.toString()
        else -> ""
    }
