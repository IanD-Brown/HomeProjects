package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.RuleDao
import io.github.iandbrown.reconciler.database.TransactionCategory
import io.github.iandbrown.reconciler.database.TransactionDao
import io.github.iandbrown.reconciler.di.inject
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readCsv
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

@Suppress("ParamsComparedByRef")
@Composable
private fun RuleEditor(viewModel: RuleViewModel = koinInject<RuleViewModel>(),
                       transCategoryViewModel:TransactionCategoryViewModel = koinInject<TransactionCategoryViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val categoryState = transCategoryViewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Rules",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch { import(categoryState.value) } },
                ButtonSettings("Export") { coroutineScope.launch { export(state.value, categoryState.value) }},
                ButtonSettings("+") { it.navigate(editor.addRoute()) })
        }) { paddingValues ->
        val categoryLookup = categoryState.value.associateBy( { it.id }, {it.name} )

        LazyVerticalGrid(
            columns = WeightedIconGridCells(2, 5, 1),
            Modifier.padding(paddingValues)
        ) {
            item { ViewText("Name") }
            item { ViewText("Type") }
            item {}
            item {}
            for (rule in state.value.sortedBy { it.match }) {
                item { ViewText(rule.match) }
                item { ViewText(categoryLookup[rule.category] ?: "") }
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
    val title = if (rule == null) "Add Rule" else "Edit Rule"
    val categoryState = transCategoryViewModel.uiState.collectAsState(emptyList())

    ViewCommon(
        title,
        description = "Return to Rules",
        bottomBar = {
            BottomBarWithButton(enabled = match.isNotEmpty()) {
                save(rule, viewModel, match, type)
                it.popBackStack()
            }
        },
        confirm = {match.isNotEmpty() && (rule == null || match != rule.match) || (rule != null && type != rule.category)},
        confirmAction = {save(rule, viewModel, match, type)},
        content = { paddingValues ->
            Row(modifier = Modifier.padding(paddingValues), content = {
                ViewTextField(value = match, label = "Name :") { match = it }
                DropdownList(
                    itemList = categoryState.value.map {it.name},
                    selectedIndex = type,
                ) { type = categoryState.value[it].id }
            })
        })
}

private fun save(rule: Rule?, viewModel: RuleViewModel, match: String, type: Int,
                 transactionDao: TransactionDao = inject<TransactionDao>().value) {
    viewModel.coroutineScope.launch {
        if (rule == null || rule.id == 0) {
            viewModel.getDao().insert(Rule(match = match.trim(), category = type))
        } else {
            if (rule.category != 0) {
                val matcher = rule.match.toRegex()
                transactionDao
                    .getByCategory(rule.category)
                    .filter { matcher.containsMatchIn(it.description) }
                    .forEach { transactionDao.setCategory(it.account, it.rowIndex, null) }
            }

            viewModel.getDao().update(Rule(rule.id, match.trim(), category = type))
        }
        if (type != 0) {
            val matcher = match.trim().toRegex()
            transactionDao.getByUnknownCategory()
                .filter { matcher.containsMatchIn(it.description) }
                .forEach { transactionDao.setCategory(it.account, it.rowIndex, type) }
        }
    }
}

private suspend fun export(rules: List<Rule>, transactionCategories: List<TransactionCategory>) {
    val file = FileKit.openFileSaver(suggestedName = "rules", extension = "csv")
    val sink = file?.sink(append = false)?.buffered()

    sink.use { bufferedSink ->
        if (bufferedSink != null) {
            val categoryLookup = transactionCategories.associateBy( { it.id }, {it.name} )
            bufferedSink.writeString("Match,Type\n")
            for (rule in rules.sortedBy { it.match }) {
                val matchString = if (rule.match.contains(',')) {
                    "\"${rule.match}\""
                } else {
                    rule.match
                }
                bufferedSink.writeString("$matchString,${categoryLookup[rule.category]}\n")
            }
        }
    }
}

private suspend fun import(transactionCategories: List<TransactionCategory>, dao: RuleDao = inject<RuleDao>().value) {
    val dataFile = FileKit.openFilePicker(FileKitType.File(listOf("csv")), mode = FileKitMode.Single)
    if (dataFile != null && dataFile.exists()) {
        dao.deleteAll()
        val categoryLookup = transactionCategories.associateBy( { it.name.uppercase() }, {it.id} )

        val df = DataFrame.readCsv(dataFile.readBytes().inputStream())
        for (row in df) {
            val category = row[1] as String?
            val categoryId = categoryLookup[category?.uppercase()]
            if (categoryId != null) {
                dao.insert(Rule(match = row[0] as String, category = categoryId))
            }
         }
    }
}
