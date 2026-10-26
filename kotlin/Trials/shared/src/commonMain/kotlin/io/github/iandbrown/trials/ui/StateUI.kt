package io.github.iandbrown.trials.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.trials.database.EmailTemplateDao
import io.github.iandbrown.trials.database.NextState
import io.github.iandbrown.trials.database.NextStateDao
import io.github.iandbrown.trials.database.State
import io.github.iandbrown.trials.database.StateDao
import io.github.iandbrown.trials.database.StateType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

internal class StateViewModel(
    dao: StateDao,
    val nextStateDao: NextStateDao,
    val emailTemplateDao: EmailTemplateDao
) : CRUDViewModel<StateDao, State>(dao = dao) {
    private val nextStatesDelegate = ReadDelegate(viewModelScope) { nextStateDao.get() }
    val nextStatesState = nextStatesDelegate.uiState

    private val templatesDelegate = ReadDelegate(viewModelScope) { emailTemplateDao.get() }
    val templatesState = templatesDelegate.uiState

    fun saveNextStates(stateName: String, selectedNextStates: Set<String>) {
        runInCoroutine {
            val existing = nextStateDao.getNextStatesFor(stateName)
            existing.forEach { nextStateDao.delete(it) }
            selectedNextStates.forEach { nextName ->
                nextStateDao.insert(NextState(stateName, nextName))
            }
            nextStatesDelegate.readAll()
        }
    }
}

@Composable
internal fun StateView() {
    val viewModel: StateViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val nextStates by viewModel.nextStatesState.collectAsState()
    val templates by viewModel.templatesState.collectAsState()

    var editingState by remember { mutableStateOf<State?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingState != null) {
        StateEditor(
            state = editingState,
            existingStates = state.values(),
            allTemplates = templates.values().map { it.name },
            currentNextStates = nextStates.values()
                .filter { it.stateName == editingState?.name }
                .map { it.nextStateName }
                .toSet(),
            onSave = { newState, selectedNextStates ->
                if (editingState == null) {
                    viewModel.insert(newState)
                } else {
                    viewModel.update(newState)
                }
                viewModel.saveNextStates(newState.name, selectedNextStates)
                editingState = null
                isAdding = false
            },
            onCancel = {
                editingState = null
                isAdding = false
            }
        )
    } else {
        ViewCommon(
            title = "States",
            states = persistentListOf(state, nextStates, templates),
            bottomBar = {
                BottomBarWithButtons(
                    addButtonSettings(onClick = { isAdding = true })
                )
            }
        ) { padding ->
            TrailingIconLazyVerticalGrid(padding, 4, 2) {
                viewTextItems(listOf("Name", "Entry Email", "Type", "Next States", "", ""))
                state.values().forEach { st ->
                    val entryEmailText = st.entryEmail ?: "-"
                    val stateTypeDisplayName = StateType.fromInt(st.type).displayName
                    val nextStatesList = nextStates.values()
                        .filter { it.stateName == st.name }
                        .joinToString(", ") { it.nextStateName }
                        .ifEmpty { "-" }
                    viewTextItems(listOf(st.name, entryEmailText, stateTypeDisplayName, nextStatesList))
                    editButton { editingState = st }
                    deleteButton { viewModel.delete(st) }
                }
            }
        }
    }
}

@Composable
internal fun StateEditor(
    state: State?,
    existingStates: List<State>,
    allTemplates: List<String>,
    currentNextStates: Set<String>,
    onSave: (State, Set<String>) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(state?.name ?: "") }
    var entryEmail by remember { mutableStateOf(state?.entryEmail ?: "") }
    var selectedType by remember { mutableStateOf(StateType.fromInt(state?.type ?: StateType.INTERMEDIATE.value)) }
    var selectedNextStates by remember { mutableStateOf(currentNextStates) }
    val isEditing = state != null

    val existingStart = existingStates.find { it.type == StateType.START.value && it.name != state?.name }
    val isStartConflict = selectedType == StateType.START && existingStart != null

    val templateOptions = (listOf("None") + allTemplates).toImmutableList()
    val selectedTemplateIndex = if (entryEmail.isBlank()) 0 else templateOptions.indexOf(entryEmail).let { if (it < 0) 0 else it }

    val stateTypeOptions = StateType.entries.map { it.displayName }.toImmutableList()
    val selectedTypeIndex = StateType.entries.indexOf(selectedType)

    val isValid = name.isNotBlank() && !isStartConflict

    ViewCommon(
        title = if (isEditing) "Edit State" else "Add State",
        description = "Cancel editing",
        bottomBar = {
            BottomBarWithButton(enabled = isValid) {
                val emailRef = if (entryEmail.isBlank() || entryEmail == "None") null else entryEmail
                onSave(State(name = name, entryEmail = emailRef, type = selectedType.value), selectedNextStates)
            }
        },
        confirm = { isValid },
        confirmAction = {
            val emailRef = if (entryEmail.isBlank() || entryEmail == "None") null else entryEmail
            onSave(State(name = name, entryEmail = emailRef, type = selectedType.value), selectedNextStates)
        }
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(name, "Name", onValueChange = { if (!isEditing) name = it })

            item { ViewText("Type") }
            item {
                DropdownList(
                    itemList = stateTypeOptions,
                    selectedIndex = selectedTypeIndex,
                    onItemClick = { idx ->
                        selectedType = StateType.entries[idx]
                    }
                )
            }

            if (existingStart != null && selectedType == StateType.START) {
                item { ViewText("Start Conflict") }
                item { ViewText("A START state already exists: ${existingStart.name}") }
            }

            item { ViewText("Entry Email") }
            item {
                DropdownList(
                    itemList = templateOptions,
                    selectedIndex = selectedTemplateIndex,
                    onItemClick = { idx ->
                        entryEmail = if (idx == 0) "" else templateOptions[idx]
                    }
                )
            }

            existingStates.map { it.name }.filter { it != name }.forEach { otherStateName ->
                val isChecked = selectedNextStates.contains(otherStateName)
                item { ViewText("Next State: $otherStateName") }
                item {
                    OutlinedTextButton(
                        value = if (isChecked) "Selected" else "Select",
                        onClick = {
                            selectedNextStates = if (isChecked) {
                                selectedNextStates - otherStateName
                            } else {
                                selectedNextStates + otherStateName
                            }
                        }
                    )
                }
            }
        }
    }
}
