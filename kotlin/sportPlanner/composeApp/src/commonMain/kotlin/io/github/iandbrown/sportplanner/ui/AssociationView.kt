package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.AssociationDao
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel

class AssociationViewModel(dao: AssociationDao) : BaseCRUDViewModel<AssociationDao, Association>(dao, { it.get() }) {
    fun save(association: Association?, name: String) {
        if (association == null)
            insert(Association(name = name.trim()))
        else
            update(Association(association.id, name.trim()))
    }
}

private const val NAME = "Name"

@Composable
fun AssociationListScreen() {
    val viewModel: AssociationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    AssociationListContent(
        state = state,
        onExport = {
            exportButtonSettings(coroutineScope, "associations") {
                toDataFrame(state.values()).writeJson(it)
            }
        },
        onImport = {
            importJsonButtonSettings(viewModel) {
                toAssociation(it)
            }
        },
        onAdd = { appNavigator.navigate(Route.AssociationEdit(null)) },
        onEdit = { appNavigator.navigate(Route.AssociationEdit(it)) },
        onDelete = { viewModel.delete(it) }
    )
}

@Composable
private fun AssociationListContent(
    state: ViewModelState<Association>,
    onExport: () -> ButtonSettings,
    onImport: () -> ButtonSettings,
    onAdd: () -> Unit,
    onEdit: (Association) -> Unit,
    onDelete: (Association) -> Unit
) {
    ViewCommon(
        "Associations",
        bottomBar = {
            BottomBarWithButtons(
                onExport(),
                onImport(),
                addButtonSettings { onAdd() }
            )
        },
        states = persistentListOf(state)) { paddingValues ->
        LazyVerticalGrid(
            columns = TrailingIconGridCells(1, 2),
            modifier = Modifier.padding(paddingValues)
        ) {
            viewTextItems(listOf("Name"))
            item(span = { GridItemSpan(2) }) {}
            for (entity in state.values().sortedBy { it.name.uppercase()}) {
                viewTextItems(listOf(entity.name))
                editButton { onEdit(entity) }
                deleteButton { onDelete(entity) }
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
fun AssociationEditScreen(association: Association?) {
    val viewModel: AssociationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()

    AssociationEditContent(
        association = association,
        state = state,
        onSave = { name ->
            viewModel.save(association, name)
            appNavigator.goBack()
        },
        onConfirmSave = { name -> viewModel.save(association, name) }
    )
}

@Composable
private fun AssociationEditContent(
    association: Association?,
    state: ViewModelState<Association>,
    onSave: (String) -> Unit,
    onConfirmSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(association?.name ?: "") }
    val title = if (association == null) "Add Association" else "Edit Association"

    ViewCommon(
        title,
        description = "Return to associations",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                onSave(name)
            }
        },
        confirm = { name.isNotEmpty() && (association == null || name != association.name) },
        confirmAction = { onConfirmSave(name) },
        states = persistentListOf(state)
    ) { paddingValues ->
        Row(modifier = Modifier.padding(paddingValues), content = {
            ViewTextField(value = name, label = "Name :") { name = it }
        })
    }
}
