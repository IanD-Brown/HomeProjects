package io.github.iandbrown.trials.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.trials.database.TrialDetailsView
import io.github.iandbrown.trials.database.TrialDetailsViewDao
import io.github.iandbrown.trials.database.TrialsView
import io.github.iandbrown.trials.database.TrialsViewDao
import io.github.iandbrown.trials.logic.DayDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.parameter.parametersOf

internal class TrialsListViewModel(dao: TrialsViewDao) :
    ReadViewModel<TrialsViewDao, TrialsView>(dao = dao)

internal class TrialDetailsViewModel(
    dao: TrialDetailsViewDao,
    @InjectedParam gender: String,
    @InjectedParam yearGroup: String,
    @InjectedParam date: Int,
) : ReadViewModel<TrialDetailsViewDao, TrialDetailsView>(
    dao = dao,
    reader = { dao.getForTrial(gender, yearGroup, date) },
) {
    fun update(confirmedEdits: Set<Long>, attendedEdits: Set<Long>) {
        viewModelScope.launch {
            try {
                confirmedEdits.forEach {
                    dao.toggleConfirmed(it)
                }
                attendedEdits.forEach {
                    dao.toggleAttended(it)
                }
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}

@Composable
internal fun TrialsListView(showDetails: (TrialsView) -> Unit) {
    val viewModel: TrialsListViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.readAll()
        }
    }

    ViewCommon(
        title = "Trials",
        states = persistentListOf(state),
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            viewTextItems(listOf("Team Category", "Date"))
            state.values().forEach { trialsView ->
                viewTextItems(
                    listOf(trialsView.name, DayDate(trialsView.date).toString()),
                    Modifier.clickable {
                        showDetails(trialsView)
                    },
                )
            }
        }
    }
}

@Composable
internal fun TrialDetails(entity: TrialsView, showDetails: (Int) -> Unit, done: () -> Unit) {
    val viewModel: TrialDetailsViewModel = koinViewModel(key = "${entity.gender.displayName}, ${entity.yearGroup.displayName} ${entity.date}") {
        parametersOf(entity.gender.displayName, entity.yearGroup.displayName, entity.date)
    }
    val state by viewModel.getState().collectAsState()
    var confirmEdits by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var attendedEdits by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var isDialogOpen by remember { mutableStateOf(false) }

    if (isDialogOpen) {
        SetStateDialog(selectedNominationIds = selected.toImmutableSet(), currentTrialDate = entity.date, onStateSet = { viewModel.readAll() }){
            isDialogOpen = false
            selected = setOf()
        }
    }

    ViewCommon(
        title = "Trial Details ${entity.name} ${DayDate(entity.date)}",
        states = persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings("Action", selected.isNotEmpty()) {
                    isDialogOpen = true
                },
                ButtonSettings(if (confirmEdits.isEmpty() && attendedEdits.isEmpty()) "Exit" else "Save and Exit"){
                    if (confirmEdits.isNotEmpty() || attendedEdits.isNotEmpty()) {
                        viewModel.update(confirmEdits, attendedEdits)
                    }
                    done()
                }
            )
        },
        confirm = { confirmEdits.isNotEmpty() || attendedEdits.isNotEmpty()},
        confirmAction = {
            if (it) {
                viewModel.update(confirmEdits, attendedEdits)
            }
        }
    ) { padding ->
        val surfaceColor = MaterialTheme.colorScheme.onSurface

        TrailingIconLazyVerticalGrid(padding, 6, 1) {
            viewTextItems(listOf("First Name", "Surname", "Confirmed", "Attended", "Outcome", "Selected", ""))
            state.values().forEach { detail ->
                viewTextItems(
                    listOf(
                        detail.firstNameOfChild,
                        detail.surnameOfChild,
                    ),
                )
                gridEntry(if (confirmEdits.contains(detail.id)) !detail.confirmed else detail.confirmed) {
                    confirmEdits = when (confirmEdits.contains(detail.id)) {
                        true -> confirmEdits - detail.id
                        false -> confirmEdits + detail.id
                    }
                }
                gridEntry(if (attendedEdits.contains(detail.id)) !detail.attended else detail.attended) {
                    attendedEdits = when (attendedEdits.contains(detail.id)) {
                        true -> attendedEdits - detail.id
                        false -> attendedEdits + detail.id
                    }
                }
                viewTextItems(listOf(detail.outcome ?: "-"))
                selectedCheckbox(selected.contains(detail.nominee), surfaceColor) {
                    selected = if (selected.contains(detail.nominee)) {
                        selected - detail.nominee
                    } else {
                        selected + detail.nominee
                    }
                }

                clickableIcon(Icons.Default.Info, "view details", surfaceColor) {
                    showDetails(detail.nominee)
                }

            }
        }
    }
}
