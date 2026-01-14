package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
fun NavigateAssociation(navController: NavController, argument: String?) {
    when (argument) {
        "View" -> AssociationEditor(navController)
        "Add" -> EditAssociation(navController, null)
        else -> EditAssociation(navController, Json.decodeFromString<Association>(argument!!))
    }
}

@Composable
private fun AssociationEditor(navController: NavController) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val state = viewModel.uiState.collectAsState()

    ViewCommon(state.value,
        navController,
        "Associations",
        { CreateFloatingAction(navController, editor.addRoute()) },
        "Return to home screen") { paddingValues ->
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
                            navController.navigate(editor.editRoute(association))
                        }, deleteClick = { viewModel.delete(association) })
                    })

            }
        })
    }
}

@Composable
private fun EditAssociation(navController: NavController, association: Association?) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(association?.name ?: "") }
    val title = if (association == null) "Add Association" else "Edit Association"

    ViewCommon(SimpleState(),
        navController,
        title,
        {},
        "Return to associations",
        {
            Button(
                onClick = {
                    save(coroutineScope, association, viewModel, name)
                    navController.popBackStack()
                }, enabled = name.isNotEmpty()
            ) { ViewText("OK") }
        },
        {name.isNotEmpty() && (association == null || name != association.name)},
        {save(coroutineScope, association, viewModel, name)}) { paddingValues ->
        Row(modifier = Modifier.padding(paddingValues), content = {
                ViewTextField(value = name, label = "Name :") { name = it }
            })
    }
}

private fun save(coroutineScope: CoroutineScope, association: Association?, viewModel: AssociationViewModel, name: String) {
    coroutineScope.launch {
        if (association == null)
            viewModel.insert(Association(name = name.trim()))
        else
            viewModel.update(Association(association.id, name.trim()))
    }
}
