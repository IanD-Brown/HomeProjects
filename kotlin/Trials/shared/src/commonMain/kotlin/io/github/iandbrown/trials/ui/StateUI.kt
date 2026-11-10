package io.github.iandbrown.trials.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
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

    fun save(state: State, isAdd: Boolean, selectedNextStates: Set<String>) {
        runInCoroutine {
            if (isAdd) {
                dao.insert(state)
            } else {
                dao.update(state)
            }
            val existing = nextStateDao.getNextStatesFor(state.name)
            existing.forEach { nextStateDao.delete(it) }
            selectedNextStates.forEach { nextName ->
                nextStateDao.insert(NextState(state.name.trim(), nextName.trim()))
            }
            nextStatesDelegate.readAll()
        }
    }
}

@Composable
internal fun StateView(showEditor: (State?, ImmutableSet<String>) -> Unit) {
    val viewModel: StateViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val nextStates by viewModel.nextStatesState.collectAsState()

    ViewCommon(
        title = "States",
        states = persistentListOf(state, nextStates),
        bottomBar = {
            BottomBarWithButtons(
                addButtonSettings(onClick = { showEditor(null, persistentSetOf()) })
            )
        }
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 4, 2) {
            viewTextItems(listOf("Name", "Entry Email", "Type", "Next States", "", ""))
            state.values().forEach { st ->
                val entryEmailText = st.entryEmail ?: "-"
                val stateTypeDisplayName = StateType.entries[st.type].displayName
                val nextStateNames = nextStates.values().filter { it.stateName == st.name }.map { it.nextStateName }
                val nextStatesList = nextStateNames.sorted().joinToString(", ").ifEmpty { "-" }
                viewTextItems(listOf(st.name, entryEmailText, stateTypeDisplayName, nextStatesList))
                editButton { showEditor(st, nextStateNames.toImmutableSet()) }
                deleteButton { viewModel.delete(st) }
            }
        }
    }
}

@Composable
internal fun StateEditor(state: State?, currentNextStates: ImmutableSet<String>, done: () -> Unit) {
    val viewModel: StateViewModel = koinViewModel()
    val states by viewModel.getState().collectAsState()
    val nextStates by viewModel.nextStatesState.collectAsState()
    val templates by viewModel.templatesState.collectAsState()

    var name by remember { mutableStateOf(state?.name ?: "") }
    var entryEmail by remember { mutableStateOf(state?.entryEmail ?: "") }
    var selectedType by remember { mutableIntStateOf(state?.type ?: StateType.INTERMEDIATE.ordinal) }
    var selectedNextStates by remember { mutableStateOf(currentNextStates.toSet()) }

    val existingStart = states.values().find { it.type == StateType.START.ordinal && it.name != state?.name }
    val isStartConflict = selectedType == StateType.START.ordinal && existingStart != null

    val existingError = states.values().find { it.type == StateType.ERROR.ordinal && it.name != state?.name }
    val isErrorConflict = selectedType == StateType.ERROR.ordinal && existingError != null

    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun setEditorState() {
        // Clean when state is null and nothing entered or entries match state
        editorState = if ((state == null && name.isBlank() && entryEmail.isBlank() && selectedType == StateType.INTERMEDIATE.ordinal && selectedNextStates.isEmpty()) ||
            (state != null && name == state.name && entryEmail == state.entryEmail && selectedType == state.type && selectedNextStates == currentNextStates)
        ) {
            EditorState.CLEAN
        } else if (name.isBlank()) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun save() {
        viewModel.save(State(name = name.trim(), entryEmail = entryEmail.ifBlank { null }, type = selectedType),
            state == null,
            selectedNextStates)
    }

    ViewCommon(
        if (state != null) "Edit State" else "Add State",
        persistentListOf(states, nextStates, templates),
        "Return to states list",
        {
            BottomBarWithButton(enabled = editorState == EditorState.VALID && !isStartConflict && !isErrorConflict) {
                save()
                done()
            }
        },
        confirm = { editorState == EditorState.VALID && !isStartConflict && !isErrorConflict },
        confirmAction = {save ->
            if (save) {
                save()
            }

            done()
        }
    ) { padding ->
        val templateOptions = (listOf("None") + templates.values().map {it.name}).toImmutableList()
        val selectedTemplateIndex = if (entryEmail.isBlank()) 0 else templateOptions.indexOf(entryEmail).let { if (it < 0) 0 else it }

        val stateTypeOptions = StateType.entries.map { it.displayName }.toImmutableList()
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(name, "Name", state != null) {
                name = it
                setEditorState()
            }

            gridEntry(stateTypeOptions, selectedType, "Type") { idx ->
                selectedType = idx
                setEditorState()
            }

            if (existingStart != null && selectedType == StateType.START.ordinal) {
                viewTextItems(listOf("Start Conflict", "A START state already exists: ${existingStart.name}"))
            }

            if (existingError != null && selectedType == StateType.ERROR.ordinal) {
                viewTextItems(listOf("Error Conflict", "An ERROR state already exists: ${existingError.name}"))
            }

            gridEntry(templateOptions, selectedTemplateIndex, "Entry Email") { idx ->
                entryEmail = if (idx == 0) "" else templateOptions[idx]
            }

            viewTextItems(listOf("", "", "Next State", "Selected"))

            states.values().map { it.name }.forEach { otherStateName ->
                val isChecked = selectedNextStates.contains(otherStateName)
                gridEntry(isChecked, otherStateName) {
                    selectedNextStates = when (it) {
                        true -> selectedNextStates + otherStateName
                        else -> selectedNextStates - otherStateName
                    }
                    setEditorState()
                }
            }
        }
    }
}
