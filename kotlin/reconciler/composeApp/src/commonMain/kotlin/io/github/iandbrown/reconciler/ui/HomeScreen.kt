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
import io.github.iandbrown.reconciler.database.Account
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.database.AccountImportDefinition
import io.github.iandbrown.reconciler.database.AccountImportDefinitionDao
import io.github.iandbrown.reconciler.database.ImportDefinition
import io.github.iandbrown.reconciler.database.ImportDefinitionDao
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.TransactionCategory
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.di.inject
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
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
private const val ACCOUNT_IMPORT_DEFINITION = "AccountImportDefinition"

private suspend fun export(
    accountDao: AccountDao = inject<AccountDao>().value,
    transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value,
    importDefinitionDao: ImportDefinitionDao = inject<ImportDefinitionDao>().value,
    accountImportDefinitionDao: AccountImportDefinitionDao = inject<AccountImportDefinitionDao>().value) {
    val file = FileKit.openFileSaver(suggestedName = "configuration", extension = "json")

    if (file != null) {
        val accounts = accountDao.getAccounts()
        val transactionCategories = transactionCategoryDao.getCategories()
        val rules = ruleDao.getRules()
        val categoryNameLookup = transactionCategories.associateBy({ it.id }, { it.name })
        val importDefinitions = importDefinitionDao.getDefinitions()
        val accountImportDefinitions = accountImportDefinitionDao.getDefinitions()
        val accountLookup = accounts.associateBy({ it.id }, { it.name })
        val importDefinitionLookup = importDefinitions.associateBy({ it.id }, { it.name })

        val dataFrame = (0 until 1).toDataFrame {
            "accounts" from {
                accounts.toDataFrame {
                    "entity" from { ACCOUNT }
                    "name" from { it.name }
                }
            }
            "transactionCategories" from {
                transactionCategories.toDataFrame {
                    "entity" from { TRANSACTION_CATEGORY }
                    "name" from { it.name }
                    "filter" from { it.filter }
                    "isSpending" from { it.isSpending }
                }
            }
            "rules" from {
                rules.toDataFrame {
                    "entity" from { RULE }
                    "match" from { it.match }
                    "category" from { categoryNameLookup[it.category] }
                }
            }
            "importDefinitions" from {
                importDefinitions.toDataFrame {
                    "entity" from { IMPORT_DEFINITION }
                    "name" from { it.name }
                }
            }
            "accountImportDefinitions" from {
                accountImportDefinitions.toDataFrame {
                    "entity" from { ACCOUNT_IMPORT_DEFINITION }
                    "account" from { accountLookup[it.accountId] }
                    "importDefinition" from { importDefinitionLookup[it.importDefinitionId] }
                    "active" from { it.active }
                    "clear" from { it.clear }
                    "sheetName" from { it.sheetName }
                    "dateColumn" from { it.dateColumn }
                    "descriptionColumn" from { it.descriptionColumn }
                    "amountInColumn" from { it.amountInColumn }
                    "amountOutColumn" from { it.amountOutColumn }
                }
            }
        }

        dataFrame.writeJson(file.toString(), true)
    }
}

private suspend fun import(
    accountDao: AccountDao = inject<AccountDao>().value,
    transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value,
    importDefinitionDao: ImportDefinitionDao = inject<ImportDefinitionDao>().value,
    accountImportDefinitionDao: AccountImportDefinitionDao = inject<AccountImportDefinitionDao>().value) {
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
                        when (it["entity"]) {
                            ACCOUNT -> accountDao.insert(Account(name = it["name"] as String))
                            TRANSACTION_CATEGORY -> transactionCategoryDao.insert(
                                TransactionCategory(
                                    name = it["name"] as String,
                                    filter = it["filter"] as Boolean,
                                    isSpending = it["isSpending"] as Boolean
                                )
                            )
                            RULE -> ruleDao.insert(Rule(
                                match = it["match"] as String,
                                category = transactionCategoryDao.getByName(it["category"] as String)!!))
                            IMPORT_DEFINITION -> importDefinitionDao.insert(ImportDefinition(
                                name = it["name"] as String))
                            ACCOUNT_IMPORT_DEFINITION -> accountImportDefinitionDao.insert(
                                AccountImportDefinition(
                                    accountDao.getByName(it["account"] as String)!!,
                                    importDefinitionDao.getByName(it["importDefinition"] as String)!!,
                                    it["active"] as Boolean,
                                    it["clear"] as Boolean,
                                    it["sheetName"] as String,
                                    it["dateColumn"] as String,
                                    it["descriptionColumn"] as String,
                                    it["amountInColumn"] as String,
                                    it["amountOutColumn"] as String
                                )
                            )
                        }
                    }
                }
            }

        }
    }
}
