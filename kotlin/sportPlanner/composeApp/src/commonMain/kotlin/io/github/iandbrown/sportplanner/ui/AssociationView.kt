package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.AssociationDao
import io.github.iandbrown.sportplanner.di.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.koinInject

class AssociationViewModel : BaseConfigCRUDViewModel<AssociationDao, Association>(inject<AssociationDao>().value)

private val editor = Editors.ASSOCIATIONS
private const val NAME = "Name"

@Composable
fun NavigateAssociation(argument: String?) {
    when (argument) {
        "View" -> AssociationEditor()
        "Add" -> EditAssociation(null)
        else -> EditAssociation(Json.decodeFromString<Association>(argument!!))
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun AssociationEditor(viewModel: AssociationViewModel = koinInject<AssociationViewModel>()) {
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        "Associations",
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "associations") {
                    toDataFrame(state.values()).writeJson(it)
                },
                importJsonButtonSettings(viewModel) {
                    toAssociation(it)
                },
                addButtonSettings { it.navigate(editor.addRoute()) }
            )
        },
        states = persistentListOf(state.value)) { paddingValues ->
        LazyVerticalGrid(
            columns = TrailingIconGridCells(1, 2),
            modifier = Modifier.padding(paddingValues)
        ) {
            viewTextItems(listOf("Name"))
            item(span = { GridItemSpan(2) }) {}
            for (entity in state.values().sortedBy { it.name.uppercase()}) {
                viewTextItems(listOf(entity.name))
                editButton { editor.editRoute(entity) }
                deleteButton { viewModel.delete(entity) }
            }
        }
    }
}

internal fun toDataFrame(rules: List<Association>): DataFrame<Association> =
    rules.toDataFrame {
        NAME from { it.name }
    }

internal fun toAssociation(row: DataRow<Any?>): Association =
    Association(name = row[NAME] as String)

@Composable
private fun EditAssociation(association: Association?) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    var name by remember { mutableStateOf(association?.name ?: "") }
    val title = if (association == null) "Add Association" else "Edit Association"
    val state = viewModel.uiState.collectAsState()

    ViewCommon(
        title,
        description = "Return to associations",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                save(association, viewModel, name)
                appNavController.popBackStack()
            }
        },
        confirm = {name.isNotEmpty() && (association == null || name != association.name)},
        confirmAction = {save(association, viewModel, name)},
        states = persistentListOf(state.value)) { paddingValues ->
            Row(modifier = Modifier.padding(paddingValues), content = {
                ViewTextField(value = name, label = "Name :") { name = it }
            })
        }
}

private fun save(association: Association?, viewModel: AssociationViewModel, name: String) {
    if (association == null)
        viewModel.insert(Association(name = name.trim()))
    else
        viewModel.update(Association(association.id, name.trim()))
 }
