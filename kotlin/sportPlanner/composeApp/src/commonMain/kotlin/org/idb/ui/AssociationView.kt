package org.idb.ui

import Association
import AssociationViewModel
import androidx.compose.foundation.clickable
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun associationEditor(/*viewModel: AssociationViewModel, */navController: NavController, back: () -> Unit) {
    val viewModel: AssociationViewModel = koinInject<AssociationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.associations.collectAsState()

    if (state.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else if (state.value.data != null) {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            TopAppBar(title = { Text("Associations") }, navigationIcon = {
                IconButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to home screen")
                }
            }, actions = {
                androidx.compose.material.IconButton(onClick = AppState.switchThemeCallback) {
                    Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
                }
            })
        }, floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("addAssociation")
            }, content = {
                Icon(
                    imageVector = Icons.Default.Add, contentDescription = "image", tint = Color.White
                )
            })
        }, content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues), content = {
                val values = state.value.data!!
                items(
                    items = values.sortedBy { it.name.uppercase().trim() },
                    key = { association -> association.id }) { association ->
                    Row(
                        modifier = Modifier.fillMaxWidth(), content = {
                            println("row for ${association.id} / ${association.name}")
                            Spacer(modifier = Modifier.size(16.dp))
                            Column(
                                modifier = Modifier.weight(2F), content = {
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = association.name,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                })
                            Spacer(modifier = Modifier.size(16.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "edit",
                                tint = Color.Green,
                                modifier = Modifier.clickable(onClick = {
                                    navController.navigate("editAssociation/${association.name}")
                                })
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "delete",
                                tint = Color.Red,
                                modifier = Modifier.clickable(onClick = {
                                    coroutineScope.launch {
                                        viewModel.delete(association)
                                    }
                                })
                            )
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
            TopAppBar(
                title = { Text("Add Association") },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to associations")
                    }
                },
            )
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    Text("Name: ")
                    TextField(
                        //modifier = Modifier.height(32.dp),
                        modifier = Modifier.weight(1F).fillMaxWidth().padding(2.dp),
                        value = name,
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            cursorColor = MaterialTheme.colors.onSurface,
                            backgroundColor = MaterialTheme.colors.surface,
                            focusedIndicatorColor = Color.Green,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        onValueChange = { name = it })
                })
        }, bottomBar = {
            Button(onClick = {
                coroutineScope.launch {
                    viewModel.insert(Association(name = name.trim()))
                    navController.popBackStack()
                }
            }) { androidx.compose.material.Text(stringResource(Res.string.ok)) }

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
            TopAppBar(
                title = { Text("Edit Association") },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to associations")
                    }
                },
            )
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    Text("Name: ")
                    TextField(
                        modifier = Modifier.weight(1F).fillMaxWidth().padding(2.dp),
                        value = name,
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            cursorColor = MaterialTheme.colors.onSurface,
                            backgroundColor = MaterialTheme.colors.surface,
                            focusedIndicatorColor = Color.Green,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        onValueChange = { name = it })
                })
        }, bottomBar = {
            Button(onClick = {
                coroutineScope.launch {
                    viewModel.rename(currentName, name.trim())
                    navController.popBackStack()
                }
            }) { androidx.compose.material.Text(stringResource(Res.string.ok)) }

        })
    }
}
