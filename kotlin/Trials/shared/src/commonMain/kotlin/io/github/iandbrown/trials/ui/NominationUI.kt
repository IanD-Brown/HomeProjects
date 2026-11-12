package io.github.iandbrown.trials.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.trials.database.AdultDetails
import io.github.iandbrown.trials.database.AdultDetailsDao
import io.github.iandbrown.trials.database.AdultType
import io.github.iandbrown.trials.database.EmailTemplate
import io.github.iandbrown.trials.database.Nomination
import io.github.iandbrown.trials.database.NominationAdultDetails
import io.github.iandbrown.trials.database.NominationAdultDetailsDao
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.database.NominationWithAdultDetails
import io.github.iandbrown.trials.database.Setting
import io.github.iandbrown.trials.database.State
import io.github.iandbrown.trials.database.StateType
import io.github.iandbrown.trials.database.StateWithNextStates
import io.github.iandbrown.trials.database.StateWithNextStatesDao
import io.github.iandbrown.trials.database.Trial
import io.github.iandbrown.trials.database.TrialDao
import io.github.iandbrown.trials.logic.DayDate
import io.github.iandbrown.trials.logic.EmailSender
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.kotlinx.dataframe.DataRow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import kotlin.time.Clock

internal class TrialViewModel(dao : TrialDao) : CRUDViewModel<TrialDao, Trial>(dao = dao), KoinComponent

internal class NominationViewModel(dao: NominationDao) : CRUDViewModel<NominationDao, Nomination>(dao = dao), KoinComponent {

    private fun resolveTemplate(field: String?, nomination: NominationWithAdultDetails, dateValue: Int) : String {
        if (field != null) {
            val regex = """#\w+\b""".toRegex()

            return regex.replace(field) { match ->
                when (match.groupValues[0]) {
                    "#Date" -> DayDate(dateValue).toString()
                    "#ChildFirstName" -> nomination.nomination.firstNameOfChild
                    "#ChildSurname" -> nomination.nomination.surnameOfChild
                    "#ParentEmail" -> nomination.adultDetailsList
                        .filter { it.type == AdultType.PARENT_OR_CARER }
                        .joinToString(",") { it.emailAddress }
                    "#NomineeEmail" -> nomination.adultDetailsList
                        .filter { it.type == AdultType.NOMINEE }
                        .joinToString(",") { it.emailAddress }
                    else -> match.groupValues[0]
                }
            }
        }
        return ""
    }

    internal fun setState(
        settings: Setting?,
        nominations: List<Nomination>,
        nextState: StateWithNextStates?,
        emailTemplate: EmailTemplate?,
        dateValue: Int,
        currentTrialDate: Int,
        onComplete: () -> Unit = {}
    ) {
        val trialsDao : TrialDao = get()
        runInCoroutine {
            val emailSender: EmailSender? = if (settings != null) get { parametersOf(settings)} else null
            try {
                val nextStateName = nextState?.state?.name
                nominations.forEach {
                    val withAdultDetails = dao.getNominationWithAdultDetails(it.id)!!
                    if (emailTemplate != null && emailSender != null) {
                        val to = resolveTemplate(emailTemplate.toTemplate, withAdultDetails, dateValue)
                        val copy = resolveTemplate(emailTemplate.copyTemplate, withAdultDetails, dateValue)
                        val subject = resolveTemplate(emailTemplate.subjectTemplate, withAdultDetails, dateValue)
                        val body = resolveTemplate(emailTemplate.bodyTemplate, withAdultDetails, dateValue)

                         emailSender.send(settings?.userName ?: "", to, copy, subject, body)
                    } else if (emailTemplate != null) {
                        LoggerFactory.get(this.javaClass.name).info {
                            "To: ${resolveTemplate(emailTemplate.toTemplate, withAdultDetails, dateValue)} " +
                                    "Copy: ${ resolveTemplate(emailTemplate.copyTemplate, withAdultDetails, dateValue)} " +
                                    "Subject ${resolveTemplate(emailTemplate.subjectTemplate, withAdultDetails, dateValue)} " +
                                    "Body: ${resolveTemplate(emailTemplate.bodyTemplate, withAdultDetails, dateValue)}\n"
                        }
                    }
                    if (currentTrialDate > 0) {
                        trialsDao.getForNominee(it.id, currentTrialDate).also { prior ->
                            trialsDao.update(prior.copy(outcome = nextStateName))
                        }
                    }
                    trialsDao.insert(Trial(it.id, dateValue))
                    dao.update(it.copy(state = nextStateName))
                }
                onComplete()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
    suspend fun insertWithAdultDetails(
        nomination: Nomination,
        adultDetailsList: List<AdultDetails>,
        errorState: State?
    ) {
        val existing = dao.getByChildDetails(nomination.firstNameOfChild, nomination.surnameOfChild, nomination.childDateOfBirth)
        val nominationId = when {
            existing == null -> dao.insert(nomination).toInt()
            errorState != null && existing.state == errorState.name && nomination.state != errorState.name -> {
                dao.delete(existing)
                dao.insert(nomination).toInt()
            }
            else -> {
                dao.update(nomination.copy(id = existing.id))
                existing.id
            }
        }

        val adultDetailsDao: AdultDetailsDao = get()
        val nominationAdultDetailsDao: NominationAdultDetailsDao = get()

        adultDetailsList.forEach { adult ->
            val existingAdult = adultDetailsDao.getByDetails(adult.emailAddress, adult.firstNameOfParentOrCarer, adult.surnameOfParentOrCarer, adult.type)
            val adultId = if (existingAdult == null) {
                adultDetailsDao.insert(adult).toInt()
            } else {
                adultDetailsDao.update(adult.copy(id = existingAdult.id))
                existingAdult.id
            }
            nominationAdultDetailsDao.insert(NominationAdultDetails(nominationId = nominationId, adultDetailsId = adultId))
        }
    }
}

internal class StateWithNextStatesViewModel(dao: StateWithNextStatesDao) : ReadViewModel<StateWithNextStatesDao, StateWithNextStates> (dao = dao)

@Composable
internal fun NominationList(showEditor: (Int) -> Unit) {
    val viewModel: NominationViewModel = koinViewModel()
    val nominationState by viewModel.getState().collectAsState()
    val stateViewModel: StateViewModel = koinViewModel()
    val nextStates by stateViewModel.getState().collectAsState()
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()
    val teamCategories by teamCategoryViewModel.getState().collectAsState()
    val startState = nextStates.values().firstOrNull { StateType.entries[it.type] == StateType.START }
    val errorState = nextStates.values().firstOrNull { StateType.entries[it.type] == StateType.ERROR }
    val endStates = nextStates.values().filter { StateType.entries[it.type] == StateType.END }.map { it.name }
    var teamCategoryFilter by remember { mutableIntStateOf(0) }
    var stateFilter by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var isDialogOpen by remember { mutableStateOf(false) }

    fun closeDialog() {
        isDialogOpen = false
    }

    if (isDialogOpen) {
        SetStateDialog( selected.toImmutableSet(), 0) { closeDialog() }
    }

    ViewCommon("Nominations",
        persistentListOf(nominationState, teamCategories),
        bottomBar = {
            BottomBarWithButtons(
                incrementalImportButtonSettings(viewModel) { row ->
                    val (nomination, adultDetails) = toNomination(row, startState, errorState)
                    viewModel.insertWithAdultDetails(nomination, adultDetails, errorState)
                },
                ButtonSettings("Action", selected.isNotEmpty()) {
                    isDialogOpen = true
                }
            )
        }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                ViewText("Filters")
                Spacer(modifier = Modifier.size(8.dp))
                val teamCategoryList = (listOf("") + teamCategories.values().map { it.name }).toImmutableList()
                DropdownList(teamCategoryList, teamCategoryFilter, modifier = Modifier.weight(1f)) {
                    teamCategoryFilter = it
                    selected = setOf()
                }
                Spacer(modifier = Modifier.size(8.dp))
                ViewText(" and/or ")
                Spacer(modifier = Modifier.size(8.dp))
                val stateList = (listOf("") + nextStates.values().map { it.name } + "Neither Error or End").toImmutableList()
                DropdownList(stateList, stateFilter, modifier = Modifier.weight(1f)) {
                    stateFilter = it
                    selected = setOf()
                }
                Spacer(modifier = Modifier.size(8.dp))
                ViewText("Selected: ${selected.size}", Modifier.weight(1f))
            }

            val (columnCount, header) = if (teamCategoryFilter == 0) {
                Pair(5,listOf("Name", "School year", "Gender", "Primary", "Secondary", "", ""))
            } else {
                Pair(3,listOf("Name", "Primary", "Secondary", "", ""))
            }
            val nominationValues = nominationState.values()
                .filter { teamCategoryFilter == 0 ||
                        it.gender == teamCategories.values()[teamCategoryFilter - 1].gender.displayName &&
                        it.whichYearGroupIsYourChildInNow == teamCategories.values()[teamCategoryFilter - 1].yearGroup.displayName }
                .filter {
                    when (stateFilter) {
                        0 -> true
                        nextStates.values().size + 1 -> {
                            (errorState != null && it.state == errorState.name) || !endStates.contains(it.state)
                        }
                        else -> {
                            it.state == nextStates.values()[stateFilter - 1].name
                        }
                    }
                }
            val surfaceColor = MaterialTheme.colorScheme.onSurface

            TrailingIconLazyVerticalGrid(PaddingValues(), columnCount, 2) {
                viewTextItems(header)
                nominationValues.forEach { nomination ->
                    if (teamCategoryFilter == 0) {
                        viewTextItems(
                            listOf(
                                "${nomination.firstNameOfChild} ${nomination.surnameOfChild.trim()}",
                                nomination.whichYearGroupIsYourChildInNow,
                                nomination.gender,
                                nomination.primaryPosition,
                                nomination.secondaryPosition
                            )
                        )
                    } else {
                        viewTextItems(
                            listOf(
                                "${nomination.firstNameOfChild} ${nomination.surnameOfChild.trim()}",
                                nomination.primaryPosition,
                                nomination.secondaryPosition
                            )
                        )
                    }
                    selectedCheckbox(selected.contains(nomination.id), surfaceColor) {
                        selected = if (selected.contains(nomination.id)) {
                            selected - nomination.id
                        } else {
                            selected + nomination.id
                        }
                    }
                    editButton { showEditor(nomination.id) }
                }
            }
        }
    }
}

@Composable
internal fun SetStateDialog(
    selectedNominationIds: ImmutableSet<Int>,
    currentTrialDate: Int,
    onStateSet: () -> Unit = {},
    closeDialog: () -> Unit
) {
    val viewModel: NominationViewModel = koinViewModel()
    val nominationState by viewModel.getState().collectAsState()
    val stateWithNextStatesViewModel: StateWithNextStatesViewModel = koinViewModel()
    val stateWithNextStatesState by stateWithNextStatesViewModel.getState().collectAsState()
    val emailTemplatesViewModel: EmailTemplateViewModel = koinViewModel()
    val emailTemplatesState by emailTemplatesViewModel.getState().collectAsState()
    val settingsModel: SettingsViewModel = koinViewModel()
    val settings by settingsModel.getState().collectAsState()
    var selectedNextState by remember { mutableStateOf<StateWithNextStates?>(null) }
    var emailDateContent by remember { mutableIntStateOf(0) }

    val nextStatesByStateName = stateWithNextStatesState.values().associateBy { it.state.name }
    val nextStates = stateWithNextStatesState.values()
        .map { it.nextStates }
        .fold(mutableSetOf<State>()) { acc, set -> acc.apply { addAll(set) } }
    val nextStateList = nextStates.toList().sortedBy { it.name }.map { it.name }.toImmutableList()

    if (nextStateList.isNotEmpty()) {
        selectedNextState = nextStatesByStateName[nextStateList[0]]
    }

    Dialog(onDismissRequest = { closeDialog() }) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    ViewText("Set state for Selection")
                }
                Spacer(Modifier.height(16.dp))
                WeightedIconLazyVerticalGrid(PaddingValues(), 0, 1, 1) {
                    gridEntry(nextStateList, 0, "Next state") {
                        selectedNextState = nextStatesByStateName[nextStateList[it]]
                    }

                    if (selectedNextState == null || selectedNextState!!.state.entryEmail == null) {
                        viewTextItems(listOf("Empty", ""))
                    } else {
                        val emailTemplate = emailTemplatesState.values()
                            .firstOrNull { it.name == selectedNextState!!.state.entryEmail }
                        if (emailTemplate?.bodyTemplate?.contains("#Date") == true || emailTemplate?.subjectTemplate?.contains(
                                "#Date"
                            ) == true
                        ) {
                            gridEntry(
                                emailDateContent,
                                "Select Date",
                                readOnly = false,
                                isSelectable = { true }) {
                                emailDateContent = it
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { closeDialog() }) { Text("Cancel") }
                    Button(onClick = {
                        viewModel.setState(
                            settings.values().firstOrNull(),
                            nominationState.values().filter { selectedNominationIds.contains(it.id) },
                            selectedNextState,
                            emailTemplatesState.values()
                                .firstOrNull { it.name == selectedNextState!!.state.entryEmail },
                            emailDateContent,
                            currentTrialDate,
                            onComplete = onStateSet
                        )
                        closeDialog()
                    }) { Text("OK") }
                }

            }
        }
    }
}

@Composable
internal fun NominationEditor(nominationId: Int, onDone: () -> Unit) {
    val viewModel: TrialViewModel = koinViewModel()
    val trialsState by viewModel.getState().collectAsState()
    val nominationViewModel: NominationViewModel = koinViewModel()
    val nominationState by nominationViewModel.getState().collectAsState()
    val stateViewModel: StateViewModel = koinViewModel()
    val statesState by stateViewModel.getState().collectAsState()

    var nomination by remember { mutableStateOf<Nomination?>(null) }
    var editorState by remember { mutableStateOf(EditorState.CLEAN) }
    var startState by remember { mutableStateOf<State?>(null) }
    var errorState by remember { mutableStateOf<State?>(null) }

    fun validate() : State? {
        if (startState == null) {
            startState = statesState.values().firstOrNull { StateType.entries[it.type] == StateType.START }
        }
        if (errorState == null) {
            errorState = statesState.values().firstOrNull { StateType.entries[it.type] == StateType.ERROR }
        }
        if (nomination == null ||
                nomination!!.firstNameOfChild.isBlank() ||
                nomination!!.surnameOfChild.isBlank() ||
                nomination!!.whichYearGroupIsYourChildInNow.isBlank() ||
                nomination!!.gender.isBlank() ||
                !validDateOfBirth(nomination!!.whichYearGroupIsYourChildInNow, DayDate(nomination!!.childDateOfBirth).asUtcMs())) {
            return errorState
        }
        return startState
    }

    fun editorNomination(): Nomination {
        val validation = validate()

        return when {
            validation == null -> Nomination()
            validation == errorState -> Nomination(state = validation.name)
            nomination?.state.isNullOrBlank() -> nomination!!.copy(state = validation.name)
            else -> nomination ?: Nomination()
        }
    }

    fun setEditorState() {
        val editorNomination = editorNomination()
        editorState = if (nomination != null && !nomination!!.state.isNullOrBlank() &&  nomination == editorNomination) {
            EditorState.CLEAN
        } else if (errorState != null && editorNomination.state == errorState!!.name) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun saveNomination() {
        nominationViewModel.update(editorNomination())
        onDone()
    }

    ViewCommon(
        title = "Nominee: ${nomination?.firstNameOfChild} ${nomination?.surnameOfChild}",
        states = persistentListOf(trialsState, nominationState, statesState),
        description = "Return to Nomination List",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID)  {
                saveNomination()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = {save ->
            if (save) {
                saveNomination()
            } else {
                onDone()
            }
        }
    ) { padding ->
        if (nomination == null) {
            nomination = nominationState.values().firstOrNull { it.id == nominationId }
        }
        val readOnly = statesState.values()
            .filter { it.name == nomination?.state }
            .map { StateType.entries[it.type].readOnly }
            .firstOrNull { it } ?: false
        Column(Modifier.padding(padding).fillMaxSize()) {
            TrailingIconLazyVerticalGrid(PaddingValues(), 2, 0) {
                gridEntry(nomination?.firstNameOfChild ?: "", "First Name", readOnly) {
                    nomination =
                        nomination?.copy(firstNameOfChild = it) ?: Nomination(firstNameOfChild = it)
                    setEditorState()
                }
                gridEntry(nomination?.surnameOfChild ?: "", "Surname", readOnly) {
                    nomination =
                        nomination?.copy(surnameOfChild = it) ?: Nomination(surnameOfChild = it)
                    setEditorState()
                }
                gridEntry(
                    nomination?.childDateOfBirth ?: 0, "Date of Birth", readOnly,
                    { validDateOfBirth(nomination?.whichYearGroupIsYourChildInNow ?: "", it) },
                    {
                        nomination = nomination?.copy(childDateOfBirth = it) ?: Nomination(
                            childDateOfBirth = it
                        )
                        setEditorState()
                    })
                gridEntry(
                    nomination?.whichYearGroupIsYourChildInNow ?: "",
                    "School Year",
                    readOnly
                ) {
                    nomination =
                        nomination?.copy(whichYearGroupIsYourChildInNow = it) ?: Nomination(
                            whichYearGroupIsYourChildInNow = it
                        )
                    setEditorState()
                }
                gridEntry(nomination?.gender ?: "", "Gender", readOnly) {
                    nomination = nomination?.copy(gender = it) ?: Nomination(gender = it)
                    setEditorState()
                }
                gridEntry(nomination?.whichSchoolDoesYourChildAttend ?: "", "School", readOnly) {
                    nomination =
                        nomination?.copy(whichSchoolDoesYourChildAttend = it) ?: Nomination(
                            whichSchoolDoesYourChildAttend = it
                        )
                    setEditorState()
                }
                gridEntry(nomination?.primaryPosition ?: "", "Primary Position", readOnly) {
                    nomination =
                        nomination?.copy(primaryPosition = it) ?: Nomination(primaryPosition = it)
                    setEditorState()
                }
                gridEntry(nomination?.secondaryPosition ?: "", "Secondary Position", readOnly) {
                    nomination = nomination?.copy(secondaryPosition = it) ?: Nomination(
                        secondaryPosition = it
                    )
                    setEditorState()
                }
                gridEntry(nomination?.foot ?: "", "Foot", readOnly) {
                    nomination = nomination?.copy(foot = it) ?: Nomination(foot = it)
                    setEditorState()
                }
                gridEntry(
                    nomination?.doesYourChildPlayForATeamOnTheWeekend ?: false,
                    "Plays for team on weekend",
                    enabled = !readOnly
                ) {
                    nomination =
                        nomination?.copy(doesYourChildPlayForATeamOnTheWeekend = it) ?: Nomination(
                            doesYourChildPlayForATeamOnTheWeekend = it
                        )
                    setEditorState()
                }
                gridEntry(nomination?.whatTypeOfTeamDoTheyPlayFor ?: "", "Type of Team", readOnly) {
                    nomination = nomination?.copy(whatTypeOfTeamDoTheyPlayFor = it) ?: Nomination(
                        whatTypeOfTeamDoTheyPlayFor = it
                    )
                    setEditorState()
                }
                gridEntry(nomination?.teamTheyPlayFor ?: "", "Team Name", readOnly) {
                    nomination =
                        nomination?.copy(teamTheyPlayFor = it) ?: Nomination(teamTheyPlayFor = it)
                    setEditorState()
                }
                gridEntry(nomination?.medicalConditions ?: "", "Medical Conditions", readOnly) {
                    nomination = nomination?.copy(medicalConditions = it) ?: Nomination(
                        medicalConditions = it
                    )
                    setEditorState()
                }
                gridEntry(
                    nomination?.iWishMyChildToAttendTrials ?: false,
                    "Wish to attend trials",
                    enabled = !readOnly
                ) {
                    nomination = nomination?.copy(iWishMyChildToAttendTrials = it) ?: Nomination(
                        iWishMyChildToAttendTrials = it
                    )
                    setEditorState()
                }
                gridEntry(
                    nomination?.consentToTheTakingOfPhotographs ?: false,
                    "Consent for photographs",
                    enabled = !readOnly
                ) {
                    nomination =
                        nomination?.copy(consentToTheTakingOfPhotographs = it) ?: Nomination(
                            consentToTheTakingOfPhotographs = it
                        )
                    setEditorState()
                }

                viewTextItems(listOf("State", nomination?.state ?: "", "", "", "Trials", ""))
            }
            TrailingIconLazyVerticalGrid(PaddingValues(), 3, 0) {
                viewTextItems(listOf("Date", "Attended", "Outcome"))
                trialsState.values().filter { it.nominee == nominationId }.sortedBy { it.date }.forEach { trial ->
                    viewTextItems(listOf(DayDate(trial.date).toString()))
                    gridEntry(trial.attended, enabled = false) {}
                    viewTextItems(listOf(trial.outcome ?: ""))
                }
            }
        }
    }
}

private fun validDateOfBirth(yearGroup: String, date: Long) : Boolean =
    validDateOfBirth( Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        .year, yearGroup, date)


internal fun validDateOfBirth(currentYear: Int, yearGroup: String, date: Long) : Boolean {
    // Date must be in the range 1st Sept -> 1st Sept
    // children in year 5 are 9-10 years old, so at the end of the year a max of 10
    // therefore if the current year is 2026 must have been born before 1st Sept 2016
    // and after 30th Oct 2015
    val startYear = when (yearGroup) {
        "Five" -> currentYear - 11
        "Six" -> currentYear - 12
        "Seven" -> currentYear - 13
        "Eight" -> currentYear - 14
        "Nine" -> currentYear - 15
        else -> 0
    }
    val startLocalDate = LocalDate(startYear, 9, 1)
    val firstDate = DayDate(startLocalDate.year, startLocalDate.dayOfYear)
    val endLocalDate = LocalDate(startYear + 1, 9, 1)
    val endDate = DayDate(endLocalDate.year, endLocalDate.dayOfYear)

    return date >= firstDate.asUtcMs() && date < endDate.asUtcMs()
}

internal fun toNomination(row: DataRow<Any?>, startState: State?, errorState: State?): Pair<Nomination, List<AdultDetails>> {
    val childBirthDate = date(row[4])
    val yearGroup = string(row[7])
    val parentOrCarerEmail = string(row[17])
    val confirmEmail = string(row[18])
    val state = when (validDateOfBirth(yearGroup, DayDate(childBirthDate).asUtcMs()) && parentOrCarerEmail == confirmEmail) {
        true -> startState?.name
        else -> errorState?.name
    }
    if (errorState != null && state == errorState.name) {
        println("${string(row[2])} ${string(row[3])} ${row[4]} $yearGroup ${DayDate(childBirthDate)}")
    }

    val adultDetails = AdultDetails(
        type = AdultType.PARENT_OR_CARER,
        emailAddress = parentOrCarerEmail,
        firstNameOfParentOrCarer = string(row[14], true),
        surnameOfParentOrCarer = string(row[15], true),
        parentOrCarerMobileNumber = string(row[16]),
        confirmEmail = confirmEmail
    )
    val nomineeDetails = AdultDetails(
        type = AdultType.NOMINEE,
        emailAddress = string(row[1])
    )
    val nomination = Nomination(
        state = state,
        firstNameOfChild = string(row[2], true),
        surnameOfChild = string(row[3], true),
        childDateOfBirth = childBirthDate,
        gender = string(row[5]),
        whichSchoolDoesYourChildAttend = string(row[6]),
        whichYearGroupIsYourChildInNow = yearGroup,
        primaryPosition = string(row[8]),
        secondaryPosition = string(row[9]),
        foot = string(row[10]),
        doesYourChildPlayForATeamOnTheWeekend = string(row[11]).trim() == "Yes",
        whatTypeOfTeamDoTheyPlayFor = string(row[12]),
        teamTheyPlayFor = string(row[13]),
        medicalConditions = string(row[19]),
        iWishMyChildToAttendTrials = string(row[20]).trim() == "Yes",
        consentToTheTakingOfPhotographs = string(row[21]).trim() == "Yes"
    )
    return Pair(nomination, listOf(adultDetails, nomineeDetails))
}

internal fun string(cell: Any?, titleCase: Boolean = false): String =
    when (cell) {
        is String -> if (titleCase) titleCase(cell.trim()) else cell.trim()
        is Double -> cell.toString()
        is Char -> cell.toString()
        else -> ""
    }

private fun titleCase(value: String) : String =
    """\b\w+\b""".toRegex().replace(value) {m ->
        m.value.lowercase().replaceFirstChar { it.uppercaseChar() }
    }

private enum class DateType(val pattern: Regex, val dayIndex: Int, val monthIndex: Int, val yearIndex: Int) {
    NUMERIC("""(\d{1,2})([-./:\s])(\d{1,2})([-./:\s])(\d{1,4})""".toRegex(), 1, 3, 5),
    STRING("""(\d{1,2})(st |nd |rd |th | )([a-zA-Z]{3,12})(\s+)(\d{1,4})""".toRegex(), 1, 3, 5),
    LONG_DIGIT("""(\d{2})(\d{2})(\d{4})""".toRegex(), 1, 2, 3),
    SHORT_DIGIT("""(\d{2})(\d{2})(\d{2})""".toRegex(), 1, 2, 3);

    fun matches(dateString: String) = pattern.containsMatchIn(dateString)

    fun dateIndex(dateString: String): Int {
        return when (val matchResult = pattern.find(dateString)) {
            null -> 0
            else -> {
                val localDate = LocalDate.orNull(
                    getYear(matchResult.groupValues[yearIndex]),
                    monthNumber(matchResult.groupValues[monthIndex]),
                    matchResult.groupValues[dayIndex].toInt())
                if (localDate == null) {
                    0
                } else {
                    DayDate(localDate.year, localDate.dayOfYear).value()
                }
            }
        }
    }
}

internal fun date(cell: Any?) : Int {
    val dateString = when (cell) {
        is String -> cell.trim()
        is Double -> BigDecimal.valueOf(cell).toPlainString()
        else -> ""
    }
    return DateType.entries.firstOrNull { it.matches(dateString) }?.dateIndex(dateString) ?: 0
}

private fun getYear(dateGroup: String): Int {
    var year = dateGroup.toInt()

    if (year < 25) {
        year += 2000
    }
    return year
}

private fun monthNumber(month: String) : Int {
    try {
        return month.toInt()
    } catch (_: NumberFormatException) {
        // ignore
    }
    return when (month.substring(0, 3).uppercase()) {
        "JAN" -> 1
        "FEB" -> 2
        "MAR" -> 3
        "APR" -> 4
        "MAY" -> 5
        "JUN" -> 6
        "JUL" -> 7
        "AUG" -> 8
        "SEP" -> 9
        "OCT" -> 10
        "NOV" -> 11
        "DEC" -> 12
        else -> 0
    }
}
