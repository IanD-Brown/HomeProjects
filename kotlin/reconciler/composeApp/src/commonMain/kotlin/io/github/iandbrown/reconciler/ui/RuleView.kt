package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.padding
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
import io.github.iandbrown.reconciler.database.AccountGroupDao
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.TransactionCategoryDao
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.di.inject
import kotlinx.coroutines.flow.MutableStateFlow
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
fun NavigateRule(viewModel: RuleViewModel = koinInject(),
                 transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                 accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val categoryState = transCategoryViewModel.uiState.collectAsState()
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    var accountGroup by remember { mutableIntStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Rules",
        bottomBar = {
            BottomBarWithButtons(
                importCsvButtonSettings(viewModel) { toRule(it) },
                exportButtonSettings(coroutineScope,"rules") { output ->
                    val value = categoryState.value.values()
                    val categoryLookup = value.associateBy({ it.id }, { it.name })
                    val groupLookup = accountGroupState.value.values().associateBy ({ it.id }, {it.name} )

                    toDataFrame(state.value.values(), categoryLookup, groupLookup).writeCsv(output)
                },
                ButtonSettings("+") { it.navigate(Rule(match = "", category = 0, accountGroup = accountGroup)) })
        },
        states = listOf(state.value, categoryState.value, accountGroupState.value)) { paddingValues ->
        val categoryLookup = categoryState.value.values().associateBy( { it.id }, {it.name} )

        LazyVerticalGrid(
            columns = WeightedIconGridCells(2, 1, 1, 1, 1, 1),
            Modifier.padding(paddingValues)
        ) {
            item(span = { GridItemSpan(2) }) { ViewText("Match")}
            item { ViewText("Account Group") }
            val value = accountGroupState.value.values()
            item { DropdownList(MutableStateFlow(value.map { it.name }),
                value.map { it.id }.indexOf(accountGroup)) {
                accountGroup = value[it].id
            }}
            viewTextItems("Category")
            item {}
            item {}
            for (rule in state.value.values().filter { it.accountGroup == accountGroup }.sortedBy { it.match }) {
                item(span = { GridItemSpan(4) }) { ViewText(rule.match) }
                viewTextItems(categoryLookup[rule.category] ?: "")
                item { EditButton { navController -> navController.navigate(rule) } }
                item { DeleteButton { viewModel.delete(rule) } }
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
        states = listOf(categoryState.value, accountGroupState.value)) { paddingValues ->
            LazyVerticalGrid(columns = WeightedIconGridCells(0, 1, 4), modifier = Modifier.padding(paddingValues)) {
                gridEntry("Value", match) {
                    match = it
                    setEditorState()
                }
                val value = categoryState.value.values()
                gridEntry(
                    "Category",
                    MutableStateFlow(value.map { it.name }),
                    value.map { it.id }.indexOf(category)
                ) {
                    category = it
                    setEditorState()
                }
                val accountGroupName =
                    accountGroupState.value.values()
                        .filter { it.id == rule.accountGroup }
                        .map { it.name }
                        .firstOrNull()
                viewTextItems("Account Group", accountGroupName ?: "")
            }
        }
}

private fun save(rule: Rule?, viewModel: RuleViewModel, match: String, type: Int, accountGroup: Int,
                 transactionDao: TransactionDao = inject<TransactionDao>().value) {
    viewModel.coroutineScope.launch {
        if (rule == null || rule.id == 0) {
            viewModel.getDao().insert(Rule(match = match.trim(), category = type, accountGroup = accountGroup))
        } else {
            if (rule.category != 0) {
                val matcher = rule.match.toRegex()
                transactionDao
                    .getByCategory(rule.category)
                    .filter { matcher.containsMatchIn(it.description) }
                    .forEach { transactionDao.setCategory(it.account, it.rowIndex, null) }
            }

            viewModel.getDao().update(Rule(rule.id, match.trim(), category = type, accountGroup = accountGroup))
        }
        if (type != 0) {
            val matcher = match.trim().toRegex()
            transactionDao.getByUnknownCategory()
                .filter { matcher.containsMatchIn(it.description) }
                .forEach { transactionDao.setCategory(it.account, it.rowIndex, type) }
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
