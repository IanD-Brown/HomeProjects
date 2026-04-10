package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.iandbrown.reconciler.database.AccountDao
import io.github.iandbrown.reconciler.database.AccountGroupDao
import io.github.iandbrown.reconciler.database.ImportDefinitionDao
import io.github.iandbrown.reconciler.database.ImportDefinitionListViewDao
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.coroutines.launch
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
    ACCOUNT_GROUPS("Account Groups"),
    IMPORT_DEFINITION("Import Definitions"),
    TRANSACTION_CATEGORY("Transaction Categories");

    fun viewRoute() : String = name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val coroutineScope = rememberCoroutineScope()
    val exceptionState = remember {mutableStateOf<Exception?>(null)}

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("Account reconcile") }, actions = {
            IconButton(onClick = {coroutineScope.launch {
                try {
                    export()
                    exceptionState.value = null
                } catch (exception: Exception) {
                    logException(javaClass.simpleName, exception, "Export failed:")
                    exceptionState.value = exception
                }
            }}) {
                Icon(Icons.Default.Upload, contentDescription = null)
            }
            IconButton(onClick = {coroutineScope.launch {
                exceptionState.value = null
                tryTransaction({
                    logException(javaClass.simpleName, it, "Import failed:")
                    exceptionState.value = it
                }, {import()})
            }}) {
                Icon(Icons.Default.Download, contentDescription = null)
            }
            IconButton(onClick = AppState.switchThemeCallback) {
                Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
            }
        })
    }, content = { paddingValues ->
        if (exceptionState.value != null) {
            AlertDialog(
                onDismissRequest = { exceptionState.value = null },
                confirmButton = { Button(onClick = { exceptionState.value = null }) { Text("OK") } },
                title = { ViewText("Error") },
                text = { ViewText(exceptionState.value!!.message ?: exceptionState.value!!.javaClass.simpleName) }
            )
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues), content = {
                items(
                    items = Editors.entries.filter { it.showOnHome }.toTypedArray(),
                    key = { entry -> entry.ordinal }) { editor ->
                    OutlinedTextButton(value = editor.displayName)
                    { appNavController.navigate(editor.viewRoute()) }
                }
            })
        }
    })
}

private const val ACCOUNT = "Account"
private const val TRANSACTION_CATEGORY = "Transaction Category"
private const val RULE = "Rule"
private const val IMPORT_DEFINITION = "ImportDefinition"
private const val ENTITY = "entity"

internal suspend fun export(accountGroupId: Int = -1,
    accountDao: AccountDao = inject<AccountDao>().value,
    transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value,
    importDefinitionDao: ImportDefinitionListViewDao = inject<ImportDefinitionListViewDao>().value,
    accountGroupDao: AccountGroupDao = inject<AccountGroupDao>().value) {
    val accounts = accountDao.getAccounts().filter { accountGroupId == -1 || it.accountGroup == accountGroupId }
    val transactionCategories = transactionCategoryDao.getCategories().filter { accountGroupId == -1 || it.accountGroup == accountGroupId }
    val rules = ruleDao.getRules().filter { accountGroupId == -1 || it.accountGroup == accountGroupId }
    val importDefinitions = importDefinitionDao.getAll().filter { accountGroupId == -1 || accounts.any {account -> account.id == it.accountId } }
    val accountGroups = accountGroupDao.getAll().filter { accountGroupId == -1 || it.id == accountGroupId }

    exportToFile("configuration", extension = "json") { output ->
        val categoryNameLookup = transactionCategories.associateBy({ it.id }, { it.name })
        val groupLookup = accountGroups.associateBy({ it.id }, { it.name })
        (0 until 1).toDataFrame {
            "accountGroups" from {
                toDataFrame(accountGroups).insert(ENTITY) { ACCOUNT_GROUP }.at(0)
            }
            "accounts" from {
                toDataFrame(accounts, groupLookup).insert(ENTITY) { ACCOUNT }.at(0)
            }
            "transactionCategories" from {
                toDataFrame(transactionCategories, groupLookup).insert(ENTITY) { TRANSACTION_CATEGORY }.at(0)
            }
            "rules" from {
                toDataFrame(rules, categoryNameLookup, groupLookup).insert(ENTITY) { RULE }.at(0)
            }
            "importDefinitions" from {
                toDataFrame(importDefinitions).insert(ENTITY) { IMPORT_DEFINITION }.at(0)
            }
        }.writeJson(output, true)
    }
}

private suspend fun import(
    accountDao: AccountDao = inject<AccountDao>().value,
    transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
    ruleDao: RuleDao = inject<RuleDao>().value,
    importDefinitionDao: ImportDefinitionDao = inject<ImportDefinitionDao>().value,
    accountGroupDao: AccountGroupDao = inject<AccountGroupDao>().value
) {
    importFromFile(
        "json",
        {
            val dataFrame = DataFrame.readJson(it)
            accountDao.deleteAll()
            transactionCategoryDao.deleteAll()
            ruleDao.deleteAll()
            importDefinitionDao.deleteAll()
            dataFrame
        },
        { row ->
            for (cell in row.values()) {
                if (cell is DataFrame<*>) {
                    cell.rows().forEach {
                        when (it[ENTITY]) {
                            ACCOUNT_GROUP -> accountGroupDao.insert(toAccountGroup(it))
                            ACCOUNT -> accountDao.insert(toAccount(it))
                            TRANSACTION_CATEGORY -> transactionCategoryDao.insert(toTransactionCategory(it))
                            RULE -> ruleDao.insert(toRule(it))
                            IMPORT_DEFINITION -> importRow(it)
                        }
                    }
                }
            }
        }
    )

}
