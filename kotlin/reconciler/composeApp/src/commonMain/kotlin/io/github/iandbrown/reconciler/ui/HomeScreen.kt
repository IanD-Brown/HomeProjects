package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.database.ImportDefinitionDao
import io.github.iandbrown.reconciler.database.ImportDefinitionListViewDao
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.di.inject
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.at
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson

enum class Editors(val displayName: String, val showOnHome: Boolean = true) {
    RULES("Rules"),
    ALL_TRANSACTIONS("All Transactions"),
    SUMMARY_BY_CATEGORY("Summary By Category"),
    SPENDING_SUMMARY("Spending Summary"),
    ACCOUNTS("Accounts"),
    IMPORT_DEFINITION("Import Definitions"),
    TRANSACTION_CATEGORY("Transaction Categories");

    fun viewRoute() : String = "$name/View"
    fun addRoute() : String = "$name/Add"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("Account reconcile") }, actions = {
            IconButton(onClick = {coroutineScope.launch { export() }}) {
                Icon(Icons.Default.Upload, contentDescription = null)
            }
            IconButton(onClick = {coroutineScope.launch { import() }}) {
                Icon(Icons.Default.Download, contentDescription = null)
            }
            IconButton(onClick = AppState.switchThemeCallback) {
                Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
            }
        })
    }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(items = Editors.entries.filter { it.showOnHome }.toTypedArray(), key = { entry -> entry.ordinal }) { editor ->
                OutlinedTextButton(value = editor.displayName)
                { appNavController.navigate(editor.viewRoute()) }
            }
        })
    })
}

private const val ACCOUNT = "Account"
private const val TRANSACTION_CATEGORY = "Transaction Category"
private const val RULE = "Rule"
private const val IMPORT_DEFINITION = "ImportDefinition"
private const val ENTITY = "entity"

private suspend fun export(
    accountDao: AccountDao = inject<AccountDao>().value,
    transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value,
    importDefinitionDao: ImportDefinitionListViewDao = inject<ImportDefinitionListViewDao>().value) {
    val file = FileKit.openFileSaver(suggestedName = "configuration", extension = "json")
    val sink = file?.sink(append = false)?.buffered()

    sink.use { bufferedSink ->
        if (bufferedSink != null) {
            val accounts = accountDao.getAccounts()
            val transactionCategories = transactionCategoryDao.getCategories()
            val rules = ruleDao.getRules()
            val categoryNameLookup = transactionCategories.associateBy({ it.id }, { it.name })
            val importDefinitions = importDefinitionDao.getAll()
            val sb = StringBuilder()

            (0 until 1).toDataFrame {
                "accounts" from {
                    toDataFrame(accounts).insert(ENTITY) {ACCOUNT}.at(0)
                }
                "transactionCategories" from {
                    toDataFrame(transactionCategories).insert(ENTITY) {TRANSACTION_CATEGORY}.at(0)
                }
                "rules" from {
                    toDataFrame(rules, categoryNameLookup).insert(ENTITY) {RULE}.at(0)
                }
                "importDefinitions" from {
                    toDataFrame(importDefinitions).insert(ENTITY) {IMPORT_DEFINITION}.at(0)
                }
            }.writeJson(sb, true)

            bufferedSink.writeString(sb.toString())
        }
    }
}

private suspend fun import(
    accountDao: AccountDao = inject<AccountDao>().value,
    transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value,
    importDefinitionDao: ImportDefinitionDao = inject<ImportDefinitionDao>().value) {
    val dataFile = FileKit.openFilePicker(FileKitType.File(listOf("json")), mode = FileKitMode.Single)

    if (dataFile != null) {
        val dataFrame = DataFrame.readJson(dataFile.readBytes().inputStream())
        accountDao.deleteAll()
        transactionCategoryDao.deleteAll()
        ruleDao.deleteAll()
        importDefinitionDao.deleteAll()

        dataFrame.rows().forEach { row ->
            for (cell in row.values()) {
                if (cell is DataFrame<*>) {
                    cell.rows().forEach {
                        when (it[ENTITY]) {
                            ACCOUNT -> accountDao.insert(toAccount(it))
                            TRANSACTION_CATEGORY -> transactionCategoryDao.insert(toTransactionCategory(it))
                            RULE -> ruleDao.insert(toRule(it))
                            IMPORT_DEFINITION -> toImportDefinition(it)
                        }
                    }
                }
            }

        }
    }
}
