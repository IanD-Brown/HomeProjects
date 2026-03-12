package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.iandbrown.reconciler.database.ImportDefinition
import io.github.iandbrown.reconciler.database.ImportDefinitionDao
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
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.io.buffered
import kotlinx.io.writeString
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.koin.compose.koinInject

class ImportDefinitionViewModel : BaseConfigCRUDViewModel<ImportDefinitionDao, ImportDefinition>(inject<ImportDefinitionDao>().value)

private enum class ImportTypes(var displayName: String) {
    FULL("Full"), CREDIT("Credit"), CURRENT("Current")
}

@Suppress("ParamsComparedByRef")
@Composable
fun ImportDefinitionListView(viewModel: ImportDefinitionViewModel = koinInject<ImportDefinitionViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "ImportDefinitions",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch { import() }},
                ButtonSettings("Export") { coroutineScope.launch { export(state.value) }},
                ButtonSettings("+") { it.navigate(ImportDefinition()) })
        }) { paddingValues ->
        LazyVerticalGrid(
            columns = WeightedIconGridCells(3, 5, 1),
            Modifier.padding(paddingValues)
        ) {
            item { ViewText("Name") }
            item { ViewText("Type") }
            item(span = { GridItemSpan(3)}) {}
            for (importDefinition in state.value.sortedBy { it.name }) {
                item { ViewText(importDefinition.name) }
                item { ViewText(ImportTypes.entries[importDefinition.type].displayName) }
                item {
                    Icon(Icons.Default.PlayArrow, "run", Modifier.clickable(onClick = { coroutineScope.launch { perform(importDefinition) }}), Color.Green)
                }
                item { EditButton { navController -> navController.navigate(importDefinition) } }
                item { DeleteButton { viewModel.delete(importDefinition) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditImportDefinition(importDefinition: ImportDefinition?, viewModel: ImportDefinitionViewModel = koinInject<ImportDefinitionViewModel>()) {
    var type by remember { mutableIntStateOf(if (importDefinition == null || importDefinition.id == 0) 0 else importDefinition.type) }
    var name by remember { mutableStateOf(importDefinition?.name ?: "") }
    var creditSheetName by remember {mutableStateOf(importDefinition?.creditSheetName ?: "") }
    var creditDateColumn by remember {mutableStateOf(importDefinition?.creditDateColumn ?: "") }
    var creditDescriptionColumn by remember {mutableStateOf(importDefinition?.creditDescriptionColumn ?: "") }
    var creditAmountInColumn by remember {mutableStateOf(importDefinition?.creditAmountInColumn ?: "") }
    var creditAmountOutColumn by remember {mutableStateOf(importDefinition?.creditAmountOutColumn ?: "") }
    var currentSheetName by remember {mutableStateOf(importDefinition?.currentSheetName ?: "") }
    var currentDateColumn by remember {mutableStateOf(importDefinition?.currentDateColumn ?: "") }
    var currentDescriptionColumn by remember {mutableStateOf(importDefinition?.currentDescriptionColumn ?: "") }
    var currentAmountInColumn by remember {mutableStateOf(importDefinition?.currentAmountInColumn ?: "") }
    var currentAmountOutColumn by remember {mutableStateOf(importDefinition?.currentAmountOutColumn ?: "") }
    var valid by remember { mutableStateOf(false) }
    val title = if (importDefinition == null) "Add ImportDefinition" else "Edit ImportDefinition"

    fun toImportDefinition() : ImportDefinition =
        ImportDefinition(importDefinition?.id ?: 0,
            type,
            name,
            creditSheetName,
            creditDateColumn,
            creditDescriptionColumn,
            creditAmountInColumn,
            creditAmountOutColumn,
            currentSheetName,
            currentDateColumn,
            currentDescriptionColumn,
            currentAmountInColumn,
            currentAmountOutColumn)

    ViewCommon(
        title,
        description = "Return to Import Definitions",
        bottomBar = {
            BottomBarWithButton(enabled = valid) {
                save(toImportDefinition(), viewModel)
                it.popBackStack()
            }
        },
        confirm = {valid && confirm(importDefinition, toImportDefinition())},
        confirmAction = {save(toImportDefinition(), viewModel)},
        ) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.padding(paddingValues)) {
            item { ViewText("Name") }
            item { ViewTextField(value = name) { name = it } }

            item { ViewText("Type") }
            item {DropdownList(
                itemList = ImportTypes.entries.map { it.displayName },
                selectedIndex = type,
            ) { type = it }}

            if (type == ImportTypes.FULL.ordinal || type == ImportTypes.CREDIT.ordinal) {
                item { ViewText("Credit Sheet Name") }
                item { ViewTextField(value = creditSheetName) {
                    creditSheetName = it
                    valid = isValid(toImportDefinition())
                } }

                item { ViewText("Credit Date Column") }
                item { ViewTextField(value = creditDateColumn) { creditDateColumn = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Credit Description Column") }
                item { ViewTextField(value = creditDescriptionColumn) { creditDescriptionColumn = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Credit Amount In Column") }
                item { ViewTextField(value = creditAmountInColumn) { creditAmountInColumn = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Credit Amount Out Column") }
                item { ViewTextField(value = creditAmountOutColumn) { creditAmountOutColumn = it
                    valid = isValid(toImportDefinition())} }
            }

            if (type == ImportTypes.FULL.ordinal || type == ImportTypes.CURRENT.ordinal) {
                item { ViewText("Current Sheet Name") }
                item { ViewTextField(value = currentSheetName) { currentSheetName = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Current Date Column") }
                item { ViewTextField(value = currentDateColumn) { currentDateColumn = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Current Description Column") }
                item { ViewTextField(value = currentDescriptionColumn) { currentDescriptionColumn = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Current Amount In Column") }
                item { ViewTextField(value = currentAmountInColumn) { currentAmountInColumn = it
                    valid = isValid(toImportDefinition())} }

                item { ViewText("Current Amount Out Column") }
                item { ViewTextField(value = currentAmountOutColumn) { currentAmountOutColumn = it
                    valid = isValid(toImportDefinition())} }
            }
        }
    }
}

private fun isValid(def: ImportDefinition) : Boolean {
    if (def.name.isNotEmpty()) {
        when (ImportTypes.entries[def.type]) {
            ImportTypes.FULL -> return def.creditSheetName.isNotEmpty() &&
                    def.creditDateColumn.isNotEmpty() &&
                    def.creditDescriptionColumn.isNotEmpty() &&
                    def.creditAmountInColumn.isNotEmpty() &&
                    def.creditAmountOutColumn.isNotEmpty() &&
                    def.currentSheetName.isNotEmpty() &&
                    def.currentDateColumn.isNotEmpty() &&
                    def.currentDescriptionColumn.isNotEmpty() &&
                    def.currentAmountInColumn.isNotEmpty() &&
                    def.currentAmountOutColumn.isNotEmpty()
            ImportTypes.CREDIT -> return def.creditSheetName.isNotEmpty() &&
                    def.creditDateColumn.isNotEmpty() &&
                    def.creditDescriptionColumn.isNotEmpty() &&
                    def.creditAmountInColumn.isNotEmpty() &&
                    def.creditAmountOutColumn.isNotEmpty()
            ImportTypes.CURRENT -> return def.currentSheetName.isNotEmpty() &&
                    def.currentDateColumn.isNotEmpty() &&
                    def.currentDescriptionColumn.isNotEmpty() &&
                    def.currentAmountInColumn.isNotEmpty() &&
                    def.currentAmountOutColumn.isNotEmpty()
        }
    }
    return false
}

private fun confirm(old: ImportDefinition?, new: ImportDefinition) : Boolean {
    if (old == null || new.type != old.type || new.name != old.name) {
        return true
    }
    return when (ImportTypes.entries[new.type]) {
        ImportTypes.FULL -> new.creditSheetName != old.creditSheetName ||
                new.creditDateColumn != old.creditDateColumn ||
                new.creditDescriptionColumn != old.creditDescriptionColumn ||
                new.creditAmountInColumn != old.creditAmountInColumn ||
                new.creditAmountOutColumn != old.creditAmountOutColumn ||
                new.currentSheetName != old.currentSheetName ||
                new.currentDateColumn != old.currentDateColumn ||
                new.currentDescriptionColumn != old.currentDescriptionColumn ||
                new.currentAmountInColumn != old.currentAmountInColumn ||
                new.currentAmountOutColumn != old.currentAmountOutColumn

        ImportTypes.CREDIT -> new .creditSheetName != old.creditSheetName ||
                new.creditDateColumn != old.creditDateColumn ||
                new.creditDescriptionColumn != old.creditDescriptionColumn ||
                new.creditAmountInColumn != old.creditAmountInColumn ||
                new.creditAmountOutColumn != old.creditAmountOutColumn

        ImportTypes.CURRENT -> new.currentSheetName != old.currentSheetName ||
                new.currentDateColumn != old.currentDateColumn ||
                new.currentDescriptionColumn != old.currentDescriptionColumn ||
                new.currentAmountInColumn != old.currentAmountInColumn ||
                new.currentAmountOutColumn != old.currentAmountOutColumn

    }
}

private fun save(importDefinition: ImportDefinition, viewModel: ImportDefinitionViewModel) {
    if (importDefinition.id == 0) {
        viewModel.insert(importDefinition)
    } else {
        viewModel.update(importDefinition)
    }
}

private suspend fun perform(importDefinition: ImportDefinition,
                            transactionDao: TransactionDao = inject<TransactionDao>().value,
                            ruleDao: RuleDao = inject<RuleDao>().value) {
    val spreadSheetFile = FileKit.openFilePicker(FileKitType.File(listOf("xlsx", "xls")), mode = FileKitMode.Single)
    if (spreadSheetFile != null && spreadSheetFile.exists()) {
        val ruleCategoryMap = ruleDao.getRules().groupBy( { it.match.toRegex() }, {it.category})
        when (ImportTypes.entries[importDefinition.type]) {
            ImportTypes.FULL -> {
                transactionDao.deleteAll()
                perform(
                    spreadSheetFile,
                    importDefinition.creditSheetName,
                    columns(ImportTypes.CREDIT, importDefinition),
                    ruleCategoryMap, transactionDao,
                    Sheet.CREDIT.number)
                perform(
                    spreadSheetFile,
                    importDefinition.currentSheetName,
                    columns(ImportTypes.CURRENT, importDefinition),
                    ruleCategoryMap, transactionDao,
                    Sheet.CURRENT.number)
            }
            ImportTypes.CREDIT -> perform(
                spreadSheetFile,
                importDefinition.creditSheetName,
                columns(ImportTypes.CREDIT, importDefinition),
                ruleCategoryMap, transactionDao,
                Sheet.CREDIT.number)
            ImportTypes.CURRENT -> perform(
                spreadSheetFile,
                importDefinition.currentSheetName,
                columns(ImportTypes.CURRENT, importDefinition),
                ruleCategoryMap, transactionDao,
                Sheet.CURRENT.number)
        }
    }
}

private fun columns(type: ImportTypes, def: ImportDefinition) =
    when (type) {
        ImportTypes.FULL -> ""
        ImportTypes.CREDIT -> "${def.creditDateColumn},${def.creditDescriptionColumn},${def.creditAmountInColumn},${def.creditAmountOutColumn}"
        ImportTypes.CURRENT -> "${def.currentDateColumn},${def.currentDescriptionColumn},${def.currentAmountInColumn},${def.currentAmountOutColumn}"
    }

private suspend fun perform(
    spreadSheetFile: PlatformFile,
    sheetName: String,
    columns: String,
    ruleCategoryMap: Map<Regex, List<Int>>,
    transactionDao: TransactionDao,
    sheetNumber: Int) {
    val df = DataFrame.readExcel(spreadSheetFile.toString(), sheetName, columns = columns)
    var rowNumber = 0
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
                    Transaction(sheetNumber, rowNumber++, date.value(), description, amount, category)
                )
            }
        }
    }
}

internal fun asDouble(value: Any?) = value as? Double ?: 0.0

internal fun description(value: Any?) = value as? String ?: "Unknown"


private suspend fun export(importDefinitions: List<ImportDefinition>) {
    val file = FileKit.openFileSaver(suggestedName = "importDefinitions", extension = "csv")
    val sink = file?.sink(append = false)?.buffered()

    sink.use { bufferedSink ->
        if (bufferedSink != null) {
            bufferedSink.writeString("Type,Name,creditSheet,creditDate,creditDescription,creditAmountIn,creditAmountOut,currentSheet,currentDate,currentDescription,currentAmountIn,currentAmountOut\n")
            for (def in importDefinitions) {
                bufferedSink.writeString(
                    "${def.type}," +
                            "${escape(def.name)}," +
                            "${escape(def.creditSheetName)}," +
                            "${escape(def.creditDateColumn)}," +
                            "${escape(def.creditDescriptionColumn)}," +
                            "${escape(def.creditAmountInColumn)}," +
                            "${escape(def.creditAmountOutColumn)}," +
                            "${escape(def.currentSheetName)}," +
                            "${escape(def.currentDateColumn)}," +
                            "${escape(def.currentDescriptionColumn)}," +
                            "${escape(def.currentAmountInColumn)},\"" +
                            "${escape(def.currentAmountOutColumn)}\n")
            }
        }
    }
}

private fun escape(string: String?) =
    if (string == null) {
        ""
    } else if (string.contains(',')) {
        "\"$string\""
    } else {
        string
    }

private suspend fun import(dao: ImportDefinitionDao = inject<ImportDefinitionDao>().value) {
    val dataFile = FileKit.openFilePicker(FileKitType.File(listOf("csv")), mode = FileKitMode.Single)
    if (dataFile != null && dataFile.exists()) {
        dao.deleteAll()

        val df = DataFrame.readCsv(dataFile.toString())
        for (row in df) {
            val type = row[0] as Int?
            if (type != null) {
                dao.insert(ImportDefinition(
                    type = type,
                    name = string(row[1]),
                    creditSheetName = string(row[2]),
                    creditDateColumn = string(row[3]),
                    creditDescriptionColumn = string(row[4]),
                    creditAmountInColumn = string(row[5]),
                    creditAmountOutColumn = string(row[6]),
                    currentSheetName = string(row[7]),
                    currentDateColumn = string(row[8]),
                    currentDescriptionColumn = string(row[9]),
                    currentAmountInColumn = string(row[10]),
                    currentAmountOutColumn = string(row[11]),
                ))
            }
        }
    }
}

private fun string(cell: Any?): String =
    when (cell) {
        is String -> cell
        is Double -> cell.toString()
        is Char -> cell.toString()
        else -> ""
    }
