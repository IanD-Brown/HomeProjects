package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCsv
import org.koin.compose.koinInject

class RuleViewModel : BaseConfigCRUDViewModel<RuleDao, Rule>(inject<RuleDao>().value) {
    fun getDao() : RuleDao = dao
}

private val editor = Editors.RULES

@Composable
fun NavigateRule(argument: String?) {
    when (argument) {
        "View" -> RuleEditor()
        "Add" -> EditRule(null)
        else -> EditRule(Json.decodeFromString<Rule>(argument!!))
    }
}

internal const val CATEGORY = "Category"
internal const val MATCH = "Match"
internal const val ACCOUNT_GROUP = "AccountGroup"

@Suppress("ParamsComparedByRef")
@Composable
private fun RuleEditor(viewModel: RuleViewModel = koinInject(),
                       transCategoryViewModel:TransactionCategoryViewModel = koinInject(),
                       accountGroupViewModel: AccountGroupViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val categoryState = transCategoryViewModel.uiState.collectAsState(emptyList())
    val accountGroupState = accountGroupViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Rules",
        bottomBar = {
            BottomBarWithButtons(
                importCsvButtonSettings(viewModel) { toRule(it) },
                exportButtonSettings(coroutineScope,"rules") { output ->
                    val categoryLookup = categoryState.value.associateBy({ it.id }, { it.name })
                    val groupLookup = accountGroupState.value.associateBy ({ it.id }, {it.name} )

                    toDataFrame(state.value, categoryLookup, groupLookup).writeCsv(output)
                },
                addButtonSettings { editor.addRoute() })
        }) { paddingValues ->
        val categoryLookup = categoryState.value.associateBy( { it.id }, {it.name} )

        LazyVerticalGrid(
            columns = WeightedIconGridCells(2, 5, 1),
            Modifier.padding(paddingValues)
        ) {
            viewTextItems("Match", "Category")
            item {}
            item {}
            for (rule in state.value.sortedBy { it.match }) {
                viewTextItems(rule.match, categoryLookup[rule.category] ?: "")
                item { EditButton { navController -> navController.navigate(rule) } }
                item { DeleteButton { viewModel.delete(rule) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
internal fun EditRule(rule: Rule?,
                      viewModel: RuleViewModel = koinInject<RuleViewModel>(),
                      transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    var match by remember { mutableStateOf(rule?.match ?: "") }
    var type by remember { mutableIntStateOf(if (rule == null || rule.id == 0) 0 else rule.category) }
    var accountGroup by remember { mutableIntStateOf(if (rule == null || rule.id == 0) 0 else rule.accountGroup) }
    val title = if (rule == null) "Add Rule" else "Edit Rule"
    val categoryState = transCategoryViewModel.uiState.collectAsState(emptyList())
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        val matchChange = match.isNotEmpty() && (rule == null || match != rule.match)
        val typeChange = type != rule?.category
        val accountGroupChange = accountGroup != rule?.accountGroup
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
                save(rule, viewModel, match, type, accountGroup)
                it.popBackStack()
            }
        },
        confirm = {editorState == EditorState.VALID},
        confirmAction = {save(rule, viewModel, match, type, accountGroup)},
        content = { paddingValues ->
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues), content = {
                gridEntry("Value", match) {
                    match = it
                    setEditorState()
                }
                gridEntry("Category", categoryState.value.map { it.name }, type) {
                    type = it
                    setEditorState()
                }
                gridEntry("Account Group", categoryState.value.map { it.name }, accountGroup) {
                    accountGroup = it
                    setEditorState()
                }
            })
        })
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
