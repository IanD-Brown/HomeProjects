package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.reconciler.database.AccountGroupDao
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

class RuleViewModel : BaseConfigCRUDViewModel<RuleDao, Rule>(inject<RuleDao>().value) {
    fun getDao() : RuleDao = dao
}

internal const val CATEGORY = "Category"
internal const val MATCH = "Match"
internal const val ACCOUNT_GROUP = "AccountGroup"

@Suppress("ParamsComparedByRef")
@Composable
fun RuleListView(viewModel: RuleViewModel = koinInject(),
                 transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                 accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val categoryState = transCategoryViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    var stateValues by remember { mutableStateOf<List<Rule>>(emptyList()) }

    ViewCommon(
        "Rules",
        bottomBar = {
            BottomBarWithButtons(
                genericButtonSettings("Reapply", viewModel) {
                    val transactionDao = inject<TransactionDao>().value
                    val transactions = transactionDao.getByAccountGroup(accountGroup)
                    val ruleCategoryMap = state.values().associateBy({ it.match.toRegex() }, { it.category })
                    transactions.forEach {
                        val category = getCategory(ruleCategoryMap, it.description)

                        if (category != it.category) {
                            transactionDao.setCategory(it.id, category)
                        }
                    }
                },
                importCsvButtonSettings(viewModel) { toRule(it) },
                exportButtonSettings(coroutineScope,"rules") { output ->
                    val value = categoryState.values()
                    val categoryLookup = value.associateBy({ it.id }, { it.name })
                    val groupLookup = accountGroupState.values().associateBy ({ it.id }, {it.name} )

                    toDataFrame(state.values(), categoryLookup, groupLookup).writeCsv(output)
                },
                ButtonSettings("+") { it.navigate(Rule(match = "", category = 0, accountGroup = accountGroup)) })
        },
        states = persistentListOf(state.value, categoryState.value, accountGroupState.value)) { paddingValues ->
        val categoryLookup = categoryState.values().associateBy( { it.id }, {it.name} )
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ViewText("Account Group")
                Spacer(modifier = Modifier.size(16.dp))
                val value = accountGroupState.values()
                DropdownList(
                    value.map { it.name }.toImmutableList(),
                    value.map { it.id }.indexOf(accountGroup)
                ) { clicked ->
                    accountGroup = value[clicked].id
                    stateValues = state.values().filter { it.accountGroup == accountGroup }.sortedBy { it.match }
                }
            }
            LazyVerticalGrid(columns = WeightedIconGridCells(2, 1, 1, 1, 1, 1)) {
                item(span = { GridItemSpan(2) }) { ViewText("Match")}
                viewTextItems(values = listOf("Category"))
                item {}
                item {}
                for (rule in stateValues) {
                    item(span = { GridItemSpan(4) }) { ViewText(rule.match) }
                    viewTextItems(values = listOf(categoryLookup[rule.category] ?: ""))
                    item { EditButton { navController -> navController.navigate(rule) } }
                    item { DeleteButton { viewModel.delete(rule) } }
                }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditRule(rule: Rule,
                      viewModel: RuleViewModel = koinInject(),
                      transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                      accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    var match by remember { mutableStateOf(rule.match) }
    var category by remember { mutableIntStateOf(rule.category) }
    var accountGroup by remember { mutableIntStateOf(rule.accountGroup) }
    val title = if (rule.id == 0) "Add Rule" else "Edit Rule"
    val categoryState = transCategoryViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        val matchChange = match.isNotEmpty() && match != rule.match
        val typeChange = category != rule.category
        val accountGroupChange = accountGroup != rule.accountGroup
        editorState = when {
            match.isEmpty() -> EditorState.DIRTY
            matchChange || typeChange || accountGroupChange -> EditorState.VALID
            else -> EditorState.CLEAN
        }
    }

    ViewCommon(
        title,
        description = "Return to Rules",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save(rule, viewModel, match, category, accountGroup)
                it.popBackStack()
            }
        },
        confirm = {editorState == EditorState.VALID},
        confirmAction = {save(rule, viewModel, match, category, accountGroup)},
        states = persistentListOf(categoryState.value, accountGroupState.value)) { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(0, 1, 4), modifier = Modifier.padding(paddingValues)) {
                gridEntry("Value", match) {
                    match = it
                    setEditorState()
                }
                val value = categoryState.values().sortedBy { it.name }
                gridEntry(
                    "Category",
                    value.map { it.name }.toImmutableList(),
                    value.map { it.id }.indexOf(category)
                ) {
                    category = value[it].id
                    setEditorState()
                }
                val accountGroupName =
                    accountGroupState.values()
                        .filter { it.id == rule.accountGroup }
                        .map { it.name }
                        .firstOrNull()
                viewTextItems(values = listOf("Account Group", accountGroupName ?: ""))
            }
        }
}

private fun save(rule: Rule?, viewModel: RuleViewModel, match: String, type: Int, accountGroup: Int,
                 transactionDao: TransactionDao = inject<TransactionDao>().value) {
    viewModel.viewModelScope.launch {
        if (rule == null || rule.id == 0) {
            viewModel.getDao().insert(Rule(match = match.trim(), category = type, accountGroup = accountGroup))
        } else {
            if (rule.category != 0) {
                val matcher = rule.match.toRegex()
                transactionDao
                    .getByCategory(rule.category)
                    .filter { matcher.containsMatchIn(it.description) }
                    .forEach { transactionDao.setCategory(it.id, null) }
            }

            viewModel.getDao().update(Rule(rule.id, match.trim(), category = type, accountGroup = accountGroup))
        }
        if (type != 0) {
            val matcher = match.trim().toRegex()
            transactionDao.getByUnknownCategory()
                .filter { matcher.containsMatchIn(it.description) }
                .forEach { transactionDao.setCategory(it.id, type) }
        }
    }
}

internal fun toDataFrame(rules: List<Rule>, categoryLookup: Map<Int, String>, groupLookup: Map<Int, String>): DataFrame<Rule> =
    rules.toDataFrame {
        MATCH from { it.match }
        CATEGORY from { categoryLookup[it.category] }
        ACCOUNT_GROUP from { groupLookup[it.accountGroup] }
    }

internal suspend fun toRule(row: DataRow<Any?>,
                            transactionCategoryDao: TransactionCategoryDao = inject<TransactionCategoryDao>().value,
                            accountGroupDao: AccountGroupDao = inject<AccountGroupDao>().value): Rule =
    Rule(match = row[MATCH] as String,
        category = transactionCategoryDao.getByName(row[CATEGORY] as String)!!,
        accountGroup = accountGroupDao.getByName(row[ACCOUNT_GROUP] as String)!!)
