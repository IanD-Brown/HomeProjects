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
import io.github.iandbrown.reconciler.di.inject
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.koin.compose.koinInject

class RuleViewModel : BaseConfigCRUDViewModel<RuleDao, Rule>(inject<RuleDao>().value)

private val editor = Editors.RULES

@Composable
fun NavigateRule(argument: String?) {
    when (argument) {
        "View" -> RuleEditor()
        "Add" -> EditRule(null)
        else -> EditRule(Json.decodeFromString<Rule>(argument!!))
    }
}

internal enum class RuleType(val displayName: String) {
    NOISE("Noise"),
    INCOME("Income"),
    OTHER("Other")
}

@Composable
private fun RuleEditor(viewModel: RuleViewModel = koinInject<RuleViewModel>()) {
    val state = viewModel.uiState.collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Rules",
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Import") { coroutineScope.launch { import() } },
                ButtonSettings("Export") { coroutineScope.launch { export(state.value) }},
                ButtonSettings("+") { it.navigate(editor.addRoute()) })
        }) { paddingValues ->
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
                item { ViewText(RuleType.entries[rule.type].displayName) }
                item { EditButton { navController -> navController.navigate(rule) } }
                item { DeleteButton { viewModel.delete(rule) } }
            }
        }
    }
}

@Composable
internal fun EditRule(rule: Rule?, viewModel: RuleViewModel = koinInject<RuleViewModel>()) {
    var match by remember { mutableStateOf(rule?.match ?: "") }
    var type by remember { mutableIntStateOf(rule?.type ?: 0) }
    val title = if (rule == null) "Add Rule" else "Edit Rule"

    ViewCommon(
        title,
        description = "Return to Rules",
        bottomBar = {
            BottomBarWithButton(enabled = match.isNotEmpty()) {
                save(rule, viewModel, match, type)
                it.popBackStack()
            }
        },
        confirm = {match.isNotEmpty() && (rule == null || match != rule.match)},
        confirmAction = {save(rule, viewModel, match, type)},
        content = { paddingValues ->
            Row(modifier = Modifier.padding(paddingValues), content = {
                ViewTextField(value = match, label = "Name :") { match = it }
                DropdownList(
                    itemList = RuleType.entries.map { it.displayName },
                    selectedIndex = type,
                ) { type = it }
            })
        })
}

private fun save(rule: Rule?, viewModel: RuleViewModel, match: String, type: Int) {
    if (rule == null || rule.id == 0)
        viewModel.insert(Rule(match = match.trim(), type = type))
    else
        viewModel.update(Rule(rule.id, match.trim(), type = type))
}

private suspend fun export(rules: List<Rule>) {
    val file = FileKit.openFileSaver(suggestedName = "rules", extension = "csv")
    val sink = file?.sink(append = false)?.buffered()

    sink.use { bufferedSink ->
        if (bufferedSink != null) {
            bufferedSink.writeString("Match,Type\n")
            for (rule in rules.sortedBy { it.match }) {
                if (rule.match.indexOf(',') >= 0) {
                    bufferedSink.writeString("\"${rule.match}\",${rule.type}\n")
                } else {
                    bufferedSink.writeString("${rule.match},${rule.type}\n")
                }
            }
        }
    }

}

private suspend fun import(dao: RuleDao = inject<RuleDao>().value) {
    val dataFile = FileKit.openFilePicker(FileKitType.File(listOf("csv")), mode = FileKitMode.Single)
    if (dataFile != null && dataFile.exists()) {
        dao.deleteAll()

        val df = DataFrame.readCsv(dataFile.toString())
        for (row in df) {
            dao.insert(Rule(match = row[0] as String, type = row[1] as Int))
        }
    }
}
