package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.AssociationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

class AssociationViewModel : BaseViewModel<AssociationDao, Association>() {
    override fun getDao(db: AppDatabase): AssociationDao = db.getAssociationDao()
}

private val editor = Editors.ASSOCIATIONS

@Composable
fun NavigateAssociation(argument: String?) {
    when (argument) {
        "View" -> AssociationEditor()
        "Add" -> EditAssociation(null)
        else -> EditAssociation(Json.decodeFromString<Association>(argument!!))
    }
}

@Composable
private fun AssociationEditor() {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val state = viewModel.uiState.collectAsState()

    ViewCommon(
        state.value,
        "Associations",
        bottomBar = {
            BottomBarWithButtonN("+") {editor.addRoute()}
        }){ paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues), content = {
                items(
                    items = state.value.data?.sortedBy { it.name.uppercase().trim() }!!,
                    key = { association -> association.id }) { association ->
                    Row(
                        modifier = Modifier.fillMaxWidth(), content = {
                            Spacer(modifier = Modifier.size(16.dp))
                            Column(
                                modifier = Modifier.weight(2F), content = {
                                    Spacer(modifier = Modifier.size(8.dp))
                                    ViewText(association.name)
                                })
                            ItemButtons(editClick = {
                                editor.editRoute(association)
                            }, deleteClick = { viewModel.delete(association) })
                        })

                }
            })
        }
}

@Composable
private fun EditAssociation(association: Association?) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(association?.name ?: "") }
    val title = if (association == null) "Add Association" else "Edit Association"

    ViewCommon(
        SimpleState(),
        title,
        description = "Return to associations",
        bottomBar = {
            BottomBarWithButton(enabled = name.isNotEmpty()) {
                save(coroutineScope, association, viewModel, name)
                appNavController.popBackStack()
            }
        },
        confirm = {name.isNotEmpty() && (association == null || name != association.name)},
        confirmAction = {save(coroutineScope, association, viewModel, name)},
        content = { paddingValues ->
            Row(modifier = Modifier.padding(paddingValues), content = {
                ViewTextField(value = name, label = "Name :") { name = it }
            })
        })
}

private fun save(coroutineScope: CoroutineScope, association: Association?, viewModel: AssociationViewModel, name: String) {
    coroutineScope.launch {
        if (association == null)
            viewModel.insert(Association(name = name.trim()))
        else
            viewModel.update(Association(association.id, name.trim()))
    }
}
