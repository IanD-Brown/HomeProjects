package org.idb.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.idb.database.AppDatabase
import org.idb.database.SeasonBreak
import org.idb.database.SeasonBreakDao
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

class SeasonBreakViewModel : BaseViewModel<SeasonBreakDao, SeasonBreak>() {
    override fun getDao(db: AppDatabase): SeasonBreakDao = db.getSeasonBreakDao()
}

private val editor : Editors = Editors.SEASON_BREAK

@Composable
fun navigateSeasonBreak(navController : NavController, argument : String?) {
    when {
        argument == null -> {}
        argument.startsWith("View&") -> seasonBreakView(navController, Json.decodeFromString<SeasonCompetitionParam>(argument.substring(5)))
        argument == "Add" -> seasonBreakEditor(navController)
        else -> seasonBreakEditor(navController, Json.decodeFromString<SeasonBreak>(argument))
    }
}

@Composable
@Preview
private fun seasonBreakView(navController: NavController, param : SeasonCompetitionParam) {
    val viewModel : SeasonBreakViewModel = koinInject()
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    viewCommon(state.value,
        navController,
        "Season breaks in ${param.seasonName}",
        { createFloatingAction(navController, editor.addRoute()) },
        content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(
                items = state.value.data?.sortedBy { it.week }!!,
                key = { seasonBreak -> seasonBreak.id }) { seasonBreak ->
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    Row(
                        modifier = Modifier.weight(2F),
                        content = {
                            spacedViewText(seasonBreak.name)
                            spacedViewText(convertMillisToDate(seasonBreak.week))
                        })
                    itemButtons(
                        { navController.navigate(editor.editRoute(seasonBreak)) },
                        { coroutineScope.launch { viewModel.delete(seasonBreak) } })
                })
            }
        })
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun seasonBreakEditor(navController: NavController, seasonBreak : SeasonBreak? = null) {
    val viewModel : SeasonBreakViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val currentName = seasonBreak?.name ?: ""
    val title = if (seasonBreak == null) "Add Season break" else "Edit Season break"
    val name = remember { mutableStateOf(currentName) }
    val week = remember {mutableStateOf(convertMillisToDate(seasonBreak?.week ?: 0L))}
    val openDialog = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    PreferableMaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { createTopBar(navController, title, "Return to season breaks") },
            content = { paddingValues ->
                Row(
                    modifier = Modifier.padding(paddingValues), content = {
                        ViewTextField(value = name.value, label = "Name:") { name.value = it }
                        val trailingIconView = @Composable {
                            IconButton(onClick = { openDialog.value = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select week")
                            }
                        }
                        ViewText("Week:")
                        TextField(
                            value = week.value,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = trailingIconView,
                            singleLine = true,
                            colors = textFieldColors(),
                            textStyle = textStyle()
                        )
                    })
                // Conditional display of the DatePickerDialog based on the openDialog state
                if (openDialog.value) {
                    // DatePickerDialog component with custom colors and button behaviors
                    DatePickerDialog(
                        onDismissRequest = {
                            // Action when the dialog is dismissed without selecting a date
                            openDialog.value = false
                        },
                        confirmButton = {
                            // Confirm button with custom action and styling
                            TextButton(
                                onClick = {
                                    // Action to set the selected date and close the dialog
                                    openDialog.value = false
                                    week.value = convertMillisToDate(datePickerState.selectedDateMillis!!)
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            // Dismiss button to close the dialog without selecting a date
                            TextButton(
                                onClick = {
                                    openDialog.value = false
                                }
                            ) {
                                Text("CANCEL")
                            }
                        }
                    ) {
                        // The actual DatePicker component within the dialog
                        DatePicker(
                            state = datePickerState,
                        )
                    }
                }
            }, bottomBar = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (seasonBreak == null) {
                                viewModel.insert(SeasonBreak(name = name.value.trim(), week = dateToMillis(week.value)))
                            } else {
                                viewModel.update(SeasonBreak(seasonBreak.id, name.value.trim(), dateToMillis(week.value)))
                            }
                            navController.popBackStack()
                        }
                    },
                    enabled = !name.value.isEmpty()
                ) { androidx.compose.material.Text(stringResource(Res.string.ok)) }
            })
    }
}