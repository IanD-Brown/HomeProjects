package io.github.iandbrown.trials.ui

import io.github.iandbrown.trials.database.EmailTemplate
import io.github.iandbrown.trials.database.EmailTemplateDao
import io.github.iandbrown.trials.database.NextState
import io.github.iandbrown.trials.database.NextStateDao
import io.github.iandbrown.trials.database.State
import io.github.iandbrown.trials.database.StateDao
import io.github.iandbrown.trials.database.StateType
import io.github.iandbrown.trials.database.StateWithNextStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeStateDao(initialStates: List<State> = emptyList()) : StateDao {
    val items = initialStates.toMutableList()

    override suspend fun get(): List<State> = items.toList()

    override suspend fun getByName(name: String): State? = items.find { it.name == name }

    override suspend fun getStateWithNextStates(name: String): StateWithNextStates? {
        val st = getByName(name) ?: return null
        return StateWithNextStates(st, emptyList())
    }

    override suspend fun insert(entity: State): Long {
        items.removeAll { it.name == entity.name }
        items.add(entity)
        return 1L
    }

    override suspend fun update(entity: State) {
        items.removeAll { it.name == entity.name }
        items.add(entity)
    }

    override suspend fun delete(entity: State) {
        items.removeAll { it.name == entity.name }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}

class FakeNextStateDao(initialNextStates: List<NextState> = emptyList()) : NextStateDao {
    val items = initialNextStates.toMutableList()

    override suspend fun get(): List<NextState> = items.toList()

    override suspend fun getNextStatesFor(stateName: String): List<NextState> =
        items.filter { it.stateName == stateName }

    override suspend fun insert(entity: NextState): Long {
        items.removeAll { it.stateName == entity.stateName && it.nextStateName == entity.nextStateName }
        items.add(entity)
        return 1L
    }

    override suspend fun update(entity: NextState) {
        insert(entity)
    }

    override suspend fun delete(entity: NextState) {
        items.removeAll { it.stateName == entity.stateName && it.nextStateName == entity.nextStateName }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}

class FakeEmailTemplateDao(initialTemplates: List<EmailTemplate> = emptyList()) : EmailTemplateDao {
    val items = initialTemplates.toMutableList()

    override suspend fun get(): List<EmailTemplate> = items.toList()

    override suspend fun getByName(name: String): EmailTemplate? = items.find { it.name == name }

    override suspend fun insert(entity: EmailTemplate): Long {
        items.removeAll { it.name == entity.name }
        items.add(entity)
        return 1L
    }

    override suspend fun update(entity: EmailTemplate) {
        items.removeAll { it.name == entity.name }
        items.add(entity)
    }

    override suspend fun delete(entity: EmailTemplate) {
        items.removeAll { it.name == entity.name }
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StateUITest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStateTypeFromOrdinal() {
        assertEquals(StateType.START, StateType.entries[0])
        assertEquals(StateType.INTERMEDIATE, StateType.entries[1])
        assertEquals(StateType.END, StateType.entries[2])
        assertEquals(StateType.ERROR, StateType.entries[3])
        assertEquals(4, StateType.entries.size)
    }

    @Test
    fun testStateViewModelInitialData() = runTest(testDispatcher) {
        val stateDao = FakeStateDao(listOf(State("Start", type = StateType.START.ordinal)))
        val nextStateDao = FakeNextStateDao(listOf(NextState("Start", "Intermediate")))
        val templateDao = FakeEmailTemplateDao(listOf(EmailTemplate("Welcome")))

        val viewModel = StateViewModel(stateDao, nextStateDao, templateDao)
        advanceUntilIdle()

        val states = viewModel.getState().value.values()
        assertEquals(1, states.size)
        assertEquals("Start", states[0].name)

        val nextStates = viewModel.nextStatesState.value.values()
        assertEquals(1, nextStates.size)
        assertEquals("Start", nextStates[0].stateName)

        val templates = viewModel.templatesState.value.values()
        assertEquals(1, templates.size)
        assertEquals("Welcome", templates[0].name)
    }

    @Test
    fun testSaveNewStateAndNextStates() = runTest(testDispatcher) {
        val stateDao = FakeStateDao()
        val nextStateDao = FakeNextStateDao()
        val templateDao = FakeEmailTemplateDao()

        val viewModel = StateViewModel(stateDao, nextStateDao, templateDao)
        advanceUntilIdle()

        val newState = State(name = "Initial", entryEmail = "Welcome", type = StateType.START.ordinal)
        viewModel.save(
            state = newState,
            isAdd = true,
            selectedNextStates = setOf("Review", "Cancelled")
        )
        advanceUntilIdle()

        val savedStates = stateDao.get()
        assertEquals(1, savedStates.size)
        assertEquals("Initial", savedStates[0].name)
        assertEquals("Welcome", savedStates[0].entryEmail)
        assertEquals(StateType.START.ordinal, savedStates[0].type)

        val savedNextStates = nextStateDao.getNextStatesFor("Initial")
        assertEquals(2, savedNextStates.size)
        assertTrue(savedNextStates.any { it.nextStateName == "Review" })
        assertTrue(savedNextStates.any { it.nextStateName == "Cancelled" })
    }

    @Test
    fun testUpdateStateAndReplaceNextStates() = runTest(testDispatcher) {
        val initialStates = listOf(State(name = "Review", type = StateType.INTERMEDIATE.ordinal))
        val initialNextStates = listOf(NextState("Review", "OldState"))
        val stateDao = FakeStateDao(initialStates)
        val nextStateDao = FakeNextStateDao(initialNextStates)
        val templateDao = FakeEmailTemplateDao()

        val viewModel = StateViewModel(stateDao, nextStateDao, templateDao)
        advanceUntilIdle()

        val updatedState = State(name = "Review", entryEmail = "ReviewTemplate", type = StateType.INTERMEDIATE.ordinal)
        viewModel.save(
            state = updatedState,
            isAdd = false,
            selectedNextStates = setOf("Approved", "Rejected")
        )
        advanceUntilIdle()

        val savedStates = stateDao.get()
        assertEquals(1, savedStates.size)
        assertEquals("ReviewTemplate", savedStates[0].entryEmail)

        val savedNextStates = nextStateDao.getNextStatesFor("Review")
        assertEquals(2, savedNextStates.size)
        assertFalse(savedNextStates.any { it.nextStateName == "OldState" })
        assertTrue(savedNextStates.any { it.nextStateName == "Approved" })
        assertTrue(savedNextStates.any { it.nextStateName == "Rejected" })
    }

    @Test
    fun testDeleteState() = runTest(testDispatcher) {
        val stateToDelete = State(name = "ToDelete", type = StateType.END.ordinal)
        val stateDao = FakeStateDao(listOf(stateToDelete))
        val nextStateDao = FakeNextStateDao()
        val templateDao = FakeEmailTemplateDao()

        val viewModel = StateViewModel(stateDao, nextStateDao, templateDao)
        advanceUntilIdle()

        viewModel.delete(stateToDelete)
        advanceUntilIdle()

        val savedStates = stateDao.get()
        assertTrue(savedStates.isEmpty())
    }

    @Test
    fun testConflictValidationLogic() {
        val existingStates = listOf(
            State("Start1", type = StateType.START.ordinal),
            State("Error1", type = StateType.ERROR.ordinal),
            State("Normal1", type = StateType.INTERMEDIATE.ordinal)
        )

        // Adding a new state as START when Start1 exists -> conflict
        val existingStartForNew = existingStates.find { it.type == StateType.START.ordinal && it.name != "NewStart" }
        assertNotNull(existingStartForNew)
        assertEquals("Start1", existingStartForNew.name)

        // Editing Start1 itself as START -> no conflict with other states
        val existingStartForSelf = existingStates.find { it.type == StateType.START.ordinal && it.name != "Start1" }
        assertNull(existingStartForSelf)

        // Adding a new state as ERROR when Error1 exists -> conflict
        val existingErrorForNew = existingStates.find { it.type == StateType.ERROR.ordinal && it.name != "NewError" }
        assertNotNull(existingErrorForNew)
        assertEquals("Error1", existingErrorForNew.name)

        // Editing Error1 itself as ERROR -> no conflict
        val existingErrorForSelf = existingStates.find { it.type == StateType.ERROR.ordinal && it.name != "Error1" }
        assertNull(existingErrorForSelf)
    }
}
