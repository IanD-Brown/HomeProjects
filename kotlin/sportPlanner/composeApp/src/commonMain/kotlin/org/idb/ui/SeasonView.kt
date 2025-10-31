package org.idb.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import kotlinx.serialization.json.Json
import org.idb.database.Season
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun navigateSeason(navController : NavController, argument : String?) {
    when (argument) {
        "View" -> seasonEditor(navController)
        "Add" -> addSeason(navController)
        else -> editSeason(navController, Json.decodeFromString<Season>(argument!!))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun seasonEditor(navController: NavController) {
    val viewModel: SeasonViewModel = koinInject<SeasonViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()

    if (state.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else if (state.value.data != null) {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            createTopBar(navController, "Seasons", "Return to home screen")
        }, floatingActionButton = {
            createFloatingAction(navController, Editors.SEASONS.name + "/Add")
        }, content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues), content = {
                val values = state.value.data!!
                items(
                    items = values.sortedBy { it.name.uppercase().trim() },
                    key = { season -> season.id }) { season ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            Row(
                                modifier = Modifier.weight(2F), content = {
                                    Spacer(modifier = Modifier.size(16.dp))
                                    ViewText(season.name)
                                    Spacer(modifier = Modifier.size(16.dp))
                                    ViewText(convertMillisToDate(season.startDate))
                                    Spacer(modifier = Modifier.size(16.dp))
                                    ViewText("to")
                                    Spacer(modifier = Modifier.size(16.dp))
                                    ViewText(convertMillisToDate(season.endDate))
                                })

                            itemButtons(
                                editClick = {
                                    navController.navigate(
                                        Editors.SEASONS.name +
                                                "/${Json.encodeToString(season)}"
                                    )
                                },
                                deleteClick = {
                                    coroutineScope.launch {
                                        viewModel.delete(season)
                                    }
                                })
                        })

                }
            })
        })
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

private enum class DateOption {NONE, START, END}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addSeason(navController: NavController) {
    val viewModel: SeasonViewModel = koinInject<SeasonViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(0L) }
    var endDate by remember { mutableStateOf(0L) }
    var showModal by remember { mutableStateOf(DateOption.NONE) }

    PreferableMaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { createTopBar(navController, "Add Season", "Return to Seasons") },
            content = { paddingValues ->
                Row(
                    modifier = Modifier.padding(paddingValues), content = {
                        ViewTextField(value = name, label = "Name :") { name = it }
                        ViewTextField(convertMillisToDate(startDate),
                            Modifier,
                            {
                                Icon(Icons.Default.DateRange, "Select date", Modifier.clickable { showModal =
                                    DateOption.START })
                            },
                            "Start date:"
                        ) {}
                        ViewTextField(convertMillisToDate(endDate),
                            Modifier,
                            {
                                Icon(Icons.Default.DateRange, "Select date", Modifier.clickable { showModal = DateOption.END })
                            },
                            "End date:"
                        ) {}
                    })
            }, bottomBar = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                        viewModel.insert(Season(name = name.trim(), startDate = startDate, endDate = endDate))
                            navController.popBackStack()
                        }
                    },
                    enabled = !name.isEmpty() && startDate > 0 && endDate > startDate
                ) { androidx.compose.material.Text(stringResource(Res.string.ok)) }
            })
        when (showModal) {
            DateOption.START -> DatePickerModal({ startDate = it }, { showModal = DateOption.NONE })
            DateOption.END -> DatePickerModal({ endDate = it }, { showModal = DateOption.NONE })
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(onDateSelected: (Long) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis!!)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editSeason(navController: NavController, editSeason: Season) {
    val viewModel: SeasonViewModel = koinInject<SeasonViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var season by remember { mutableStateOf(editSeason) }
    var name by remember { mutableStateOf(season.name) }
    var startDate by remember { mutableStateOf(editSeason.startDate) }
    var endDate by remember { mutableStateOf(editSeason.endDate) }
    var showModal by remember { mutableStateOf(DateOption.NONE) }

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            createTopBar(navController, "Edit Season", "Return to Seasons")
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    ViewTextField(value = name, label = "Name :") { name = it }
                    ViewTextField(convertMillisToDate(startDate),
                        Modifier,
                        {
                            Icon(Icons.Default.DateRange, "Select date", Modifier.clickable { showModal =
                                DateOption.START })
                        },
                        "Start date:"
                    ) {}
                    ViewTextField(convertMillisToDate(endDate),
                        Modifier,
                        {
                            Icon(Icons.Default.DateRange, "Select date", Modifier.clickable { showModal = DateOption.END })
                        },
                        "End date:"
                    ) {}
                })
        }, bottomBar = {
            Button(onClick = {
                coroutineScope.launch {
                    viewModel.update(Season(season.id, name.trim(), startDate, endDate))
                    navController.popBackStack()
                }
            },
                enabled = !name.isEmpty() && startDate > 0 && endDate > startDate) { androidx.compose.material.Text(stringResource(Res.string.ok)) }

        })
        when (showModal) {
            DateOption.START -> DatePickerModal({ startDate = it }, { showModal = DateOption.NONE })
            DateOption.END -> DatePickerModal({ endDate = it }, { showModal = DateOption.NONE })
            else -> {}
        }
    }
}
