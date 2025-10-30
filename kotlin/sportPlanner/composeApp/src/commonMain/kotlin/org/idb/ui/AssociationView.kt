package org.idb.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import org.idb.database.Association
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun navigateAssociation(navController: NavController, argument: String?) {
    when (argument) {
        "View" -> associationEditor(navController)
        "Add" -> addAssociation(navController)
        else -> editAssociation(navController, argument!!)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun associationEditor(navController: NavController) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.associations.collectAsState()

    if (state.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else if (state.value.data != null) {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            createTopBar(navController, "Associations", "Return to home screen")
        }, floatingActionButton = {
            createFloatingAction(navController, Editors.ASSOCIATIONS.name + "/Add")
        }, content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues), content = {
                val values = state.value.data!!
                items(
                    items = values.sortedBy { it.name.uppercase().trim() },
                    key = { association -> association.id }) { association ->
                    Row(
                        modifier = Modifier.fillMaxWidth(), content = {
                            Spacer(modifier = Modifier.size(16.dp))
                            Column(
                                modifier = Modifier.weight(2F), content = {
                                    Spacer(modifier = Modifier.size(8.dp))
                                    ViewText(association.name)
                                })
                            itemButtons(editClick = {
                                navController.navigate("${Editors.ASSOCIATIONS.name}/${association.name}")
                            }, deleteClick = {
                                coroutineScope.launch {
                                    viewModel.delete(association)
                                }
                            })
                        })

                }
            })
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addAssociation(navController: NavController) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            createTopBar(navController, "Add Association", "Return to associations")
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    Text("Name: ")
                    ViewTextField(value = name) { name = it.trim() }
                })
        }, bottomBar = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.insert(Association(name = name.trim()))
                        navController.popBackStack()
                    }
                }, enabled = !name.isEmpty()
            ) { androidx.compose.material.Text(stringResource(Res.string.ok)) }

        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editAssociation(navController: NavController, currentName: String) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(currentName) }

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            createTopBar(navController, "Edit Association", "Return to associations")
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    Text("Name: ")
                    ViewTextField(value = name) { name = it.trim() }
                })
        }, bottomBar = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.rename(currentName, name.trim())
                        navController.popBackStack()
                    }
                }, enabled = !name.isEmpty()
            ) { androidx.compose.material.Text(stringResource(Res.string.ok)) }
        })
    }
}
