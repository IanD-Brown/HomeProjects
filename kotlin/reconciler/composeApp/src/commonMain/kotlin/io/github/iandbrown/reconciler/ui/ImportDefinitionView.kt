package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import dev.shivathapaa.logger.api.LoggerFactory
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
import io.github.iandbrown.reconciler.di.koinApp
import io.github.iandbrown.reconciler.logic.DayDate
import io.github.iandbrown.reconciler.logic.PDFConverterInterface
import io.github.iandbrown.reconciler.logic.Range
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.JoinType
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.join
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter


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
private const val FILE_TYPE = "FileType"

private enum class ImportTypes(val displayName: String) {
    EXCEL("Excel"),
    CREDIT("Santander credit HTML"),
    CURRENT("Santander current PDF")
}

class ImportDefinitionViewModel : BaseConfigCRUDViewModel<ImportDefinitionDao, ImportDefinition>(inject<ImportDefinitionDao>().value) {
    suspend fun save(importId: Int, name: String, type: Int, importDefinitions: (Int) -> List<AccountImportDefinition>) : Boolean {
        try {
            dao.save(importId, name, type, importDefinitions)
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
                    item(span = { GridItemSpan(2) }) { ViewText(item.name) }
                    item(span = { GridItemSpan(5)}) { ViewText(ImportTypes.entries[item.type].displayName)}
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
    var type by remember { mutableIntStateOf(importDefinitionListView.type) }
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
                } else {
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
        importDefinitionState.value !is ViewModelState.Error && (name != importDefinitionListView.name || type != importDefinitionListView.type ||
                listOf(activeEdits, clearEdits, sheetNameEdits, descriptionEdits, dateEdits, amountInEdits, amountOutEdits).none {it.isNotEmpty()})

    ViewCommon(
        title,
        description = "Return to Import Definitions",
        bottomBar = {
            if (importDefinitionState.value !is ViewModelState.Error) {
                BottomBarWithButton(enabled = valid) { navController ->
                    coroutineScope.launch {
                        if (importDefinitionViewModel.save(importDefinitionListView.importDefinitionId, name, type)
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
                importDefinitionViewModel.save(importDefinitionListView.importDefinitionId, name, type) {
                    toImportDefinitions(it)
                }
            }
        },
        states = listOf(definitionState.value, accountState.value, importDefinitionState.value),
    ) { paddingValues ->
        val editingDefinitions = definitionState.value.values()
            .filter { it.importDefinitionId == importDefinitionListView.importDefinitionId }
            .associateBy { it.accountId }
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ViewText("Name")
                ViewTextField(name) {name = it}
                ViewText("Type")
                DropdownList(MutableStateFlow(ImportTypes.entries.map { it.displayName }), type) {
                    type = it
                    setValid()
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(8)) {
                viewTextItems("Account", "Active", "Clear", "Sheet Name", "Date Column", "Description Column", "Amount In Column", "Amount Out Column")
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
}

private suspend fun perform(exceptionHandler: (Exception) -> Unit,
    importDefinitions: List<ImportDefinitionListView>,
    transactionDao: TransactionDao = inject<TransactionDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value
) {
    tryTransaction(exceptionHandler, {
        val importType = ImportTypes.entries[importDefinitions[0].type]
        val extensions = when (importType) {
            ImportTypes.EXCEL -> listOf("xlsx", "xls")
            ImportTypes.CREDIT -> listOf("html")
            ImportTypes.CURRENT -> listOf("pdf")
        }
        val importFile = FileKit.openFilePicker(FileKitType.File(extensions), mode = FileKitMode.Single)
        if (importFile != null && importFile.exists()) {
            val ruleCategoryMap = ruleDao.getRules().associateBy({ it.match.toRegex() }, { it.category })
            for (importDefinition in importDefinitions) {
                if (importDefinition.clear) {
                    transactionDao.deleteAllByAccount(importDefinition.accountId)
                }
                val df = when (importType) {
                    ImportTypes.CREDIT -> readHtml(importFile, importDefinition)
                    ImportTypes.CURRENT -> readPdf(importFile, importDefinition)
                    ImportTypes.EXCEL -> readExcel(importFile, importDefinition)
                }
                performDataFrame(df, ruleCategoryMap, importDefinition)
            }
        }
    })
}

private suspend fun readPdf(importFile: PlatformFile, importDefinition: ImportDefinitionListView) : DataFrame<Any?> {
    val logger = LoggerFactory.get(ImportDefinitionViewModel::class.simpleName!!)
    val sourceBytes = importFile.readBytes()
    val converter: PDFConverterInterface = koinApp.koin.get { parametersOf(sourceBytes)}
    val dateRange = converter.getDateRange()
    val datePattern = "(\\d{1,2})(st |nd |rd |th )([a-zA-Z]{3})".toRegex()
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
    var transactionDate: Long = 0

    logger.debug {"Range ${converter.getDateRange()}"}
    val rows = converter.rowContent { it.size >= 4 }

    val headerRow = rows.firstOrNull { row -> row.values.toSet().containsAll(
        listOf(importDefinition.dateColumn,
        importDefinition.descriptionColumn,
            importDefinition.amountInColumn,
            importDefinition.amountOutColumn)) }
    logger.debug {"HeaderRow $headerRow"}
    val dateIndex = getRange(importDefinition.dateColumn, headerRow!!)
    val descriptionIndex = getRange(importDefinition.descriptionColumn, headerRow)
    val amountInIndex = getRange(importDefinition.amountInColumn, headerRow)
    val amountOutIndex = getRange(importDefinition.amountOutColumn, headerRow)
    logger.debug {"HeaderRanges   $dateIndex\n   $descriptionIndex\n   $amountInIndex\n   $amountOutIndex"}
    var df = DataFrame.emptyOf<Any?>()
    for (row in rows.filter { it.size >= 4 }) {
        val dateColumn = getByRange(dateIndex, row)
        val amountIn = getByRange(amountInIndex, row).replace(Regex(","), "").toDoubleOrNull()
        val amountOut = getByRange(amountOutIndex, row).replace(Regex(","), "").toDoubleOrNull()
        if (datePattern.matches(dateColumn) && (amountIn != null || amountOut != null)) {
            val dateParts = datePattern.matchEntire(dateColumn)?.groupValues!!
            val dayNumber = dateParts[1].toInt().toString().padStart(2, '0')
            var date  = LocalDate.parse("$dayNumber-${dateParts[3]}-${dateRange.first}", dateFormatter)
            if (date.toEpochDay() < transactionDate) {
                date  = LocalDate.parse("$dayNumber-${dateParts[3]}-${dateRange.second}", dateFormatter)
            }
            transactionDate = date.toEpochDay()
            val amount = (amountIn ?: 0.0) - (amountOut ?: 0.0)
            df = df.concat(dataFrameOf("A", "B", "C")(DayDate(date), getByRange(descriptionIndex, row), amount))
        } else {
            logger.debug {"$dateColumn ${getByRange(descriptionIndex, row)} $amountIn $amountOut"}
            for (cell in row) {
                logger.debug {"Pair(Range(${cell.key.from}F, ${cell.key.to}F), \"${cell.value}\"),"}
            }
        }
    }
    return df
}

private fun getRange(content: String, row: Map<Range, String>) : Range
    = row.filter { it.value == content }.keys.first()

private fun asIntOneDecimal(position: Float) = (position * 10.0F).toInt()

internal fun getByRange(range: Range, row: Map<Range, String>) : String {
    val intFrom = asIntOneDecimal(range.from)
    val intTo = asIntOneDecimal(range.to)
    return row.filter { asIntOneDecimal(it.key.from) in intFrom..intTo }
        .values.fold("") {acc, it -> acc + it}
}

private fun columns(def: ImportDefinitionListView) =
    "${def.dateColumn},${def.descriptionColumn},${def.amountInColumn},${def.amountOutColumn}"

private fun toTextArray(elements: Elements) : List<String> {
    val result = mutableListOf<String>()
    elements.forEach { result.add(it.text()) }
    return result
}

private fun rows(table: Element) : List<List<String>>
    = table.select("tbody tr")
        .map{ it.select("td, th") }
        .map { toTextArray(it) }

private suspend fun readHtml(spreadSheetFile: PlatformFile, definition: ImportDefinitionListView) : DataFrame<Any?> {
    val htmlString = spreadSheetFile.readString()
    if (htmlString.isBlank()) return DataFrame.emptyOf<Any?>()

    val doc = Jsoup.parse(htmlString)
    val table = doc.select("table").first()
    val dateIndex = definition.dateColumn[0] - 'A'
    val descriptionIndex = definition.descriptionColumn[0] - 'A'
    val amountInIndex = definition.amountInColumn[0] - 'A'
    val amountOutIndex = definition.amountOutColumn[0] - 'A'
    val datePattern = "[0-9]{4}-[0-9]{2}-[0-9]{2}".toRegex()
    val dateFormatPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return rows(table!!)
        .filter {datePattern.matches(it[dateIndex]) && htmlTextAsDouble(it[amountInIndex]) - htmlTextAsDouble(it[amountOutIndex]) != 0.0}
        .fold(DataFrame.emptyOf<Any?>()) {acc, it -> acc.concat(dataFrameOf("A", "B", "C")(
            DayDate(LocalDate.parse(it[dateIndex], dateFormatPattern)),
            description(it[descriptionIndex]),
            htmlTextAsDouble(it[amountInIndex]) - htmlTextAsDouble(it[amountOutIndex])))}
}

private fun htmlTextAsDouble(text: String) : Double = when {
    text.startsWith("\ufffd ") -> text.substring(2).replace(Regex(","), "").toDouble()
    text.isEmpty() -> 0.0
    else -> text.toDouble()
}

private suspend fun readExcel(spreadSheetFile: PlatformFile, definition: ImportDefinitionListView) : DataFrame<Any?> {
    val df = DataFrame.readExcel(spreadSheetFile.readBytes().inputStream(), definition.sheetName, columns = columns(definition))
    var result = DataFrame.emptyOf<Any?>()
    df.rows()
        .filter { it[0] is LocalDateTime }
        .filter { asDouble(it[2]) - asDouble(it[3]) != 0.0 }
        .forEach {
            result = result.concat(dataFrameOf("A", "B", "C")(
                DayDate(it[0] as LocalDateTime),
                description(it[1]),
                asDouble(it[2]) - asDouble(it[3])))
        }
    return result
}

private suspend fun performDataFrame(dataFrame: AnyFrame,
                                     ruleCategoryMap: Map<Regex, Int>,
                                     definition: ImportDefinitionListView,
                                     transactionDao: TransactionDao = inject<TransactionDao>().value) {
    var imported = 0
    dataFrame.rows()
        .map {
            val description = description(it[1])
            val category = ruleCategoryMap.entries.firstOrNull { entry -> entry.key.containsMatchIn(description) }?.value
            Transaction(
                account = definition.accountId,
                date = (it[0] as DayDate).value(),
                description = description,
                amount = asDouble(it[2]),
                category = category
            )
        }
        .filter { definition.clear || !transactionDao.exists(it.account, it.description, it.date, it.amount) }
        .forEach {
            transactionDao.insert(it)
            imported++
        }
    val logger = LoggerFactory.get(transactionDao::class.simpleName!!)
    logger.info { "${definition.sheetName} imported $imported" }
}

internal fun asDouble(value: Any?) = value as? Double ?: 0.0

internal fun description(value: Any?) = value as? String ?: "Unknown"

internal fun toDataFrame(importDefinitions: List<ImportDefinitionListView>): DataFrame<ImportDefinitionListView> =
    importDefinitions.groupBy { it.importDefinitionId }.map { it.value.first() }
        .toDataFrame {
            TYPE from { DEFINITION }
            DEFINITION_NAME from { it.name }
            FILE_TYPE from {it.type.toString()}
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
        DEFINITION -> dao.insert(ImportDefinition(name = string(row[DEFINITION_NAME]), type = string(row[FILE_TYPE]).toInt()))
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
