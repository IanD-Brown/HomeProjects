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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
        dateValue: Int
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
                    trialsDao.getForNominee(it.id).firstOrNull()?.also {prior ->
                        trialsDao.update(prior.copy(outcome = nextStateName))
                    }
                    trialsDao.insert(Trial(it.id, dateValue))
                    update(it.copy(state = nextStateName))
                }
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
internal fun NominationList(showEditor: (Nomination) -> Unit) {
    val viewModel: NominationViewModel = koinViewModel()
    val nominationState by viewModel.getState().collectAsState()
    val settingsModel: SettingsViewModel = koinViewModel()
    val settings by settingsModel.getState().collectAsState()
    val stateViewModel: StateViewModel = koinViewModel()
    val nextStates by stateViewModel.getState().collectAsState()
    val stateWithNextStatesViewModel: StateWithNextStatesViewModel = koinViewModel()
    val stateWithNextStatesState by stateWithNextStatesViewModel.getState().collectAsState()
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()
    val teamCategories by teamCategoryViewModel.getState().collectAsState()
    val emailTemplatesViewModel: EmailTemplateViewModel = koinViewModel()
    val emailTemplatesState by emailTemplatesViewModel.getState().collectAsState()
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
        var selectedNextState by remember { mutableStateOf<StateWithNextStates?>(null) }
        var emailDateContent by remember { mutableIntStateOf(0) }

        val nextStatesByStateName = stateWithNextStatesState.values().associateBy { it.state.name }
        var nextStates = stateWithNextStatesState.values()
            .map {it.nextStates}
            .fold(mutableSetOf<State>()) { acc, set -> acc.apply { addAll(set) } }

        val selectedNominations = nominationState.values().filter { selected.contains(it.id) }
        selectedNominations
            .forEach {
                nextStates = nextStates.intersect(nextStatesByStateName[it.state]?.nextStates?.toSet()
                    ?: emptySet()).toMutableSet()
            }
        val nextStateList = nextStates.toList().sortedBy { it.name }.map {it.name}.toImmutableList()

        if (nextStateList.isNotEmpty()) {
            selectedNextState = nextStatesByStateName[nextStateList[0]]
        }

        Dialog(onDismissRequest = { closeDialog() }/*, DialogProperties(dismissOnClickOutside = true)*/) {
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
                        TextButton(onClick = { closeDialog()}) { Text("Cancel") }
                        Button(onClick = {
                            viewModel.setState(
                                settings.values().firstOrNull(),
                                selectedNominations,
                                selectedNextState,
                                emailTemplatesState.values().firstOrNull { it.name == selectedNextState!!.state.entryEmail },
                                emailDateContent)
                            closeDialog()
                        }) { Text("OK") }
                    }

                }
            }
        }
    }

    ViewCommon("Nominations",
        persistentListOf(nominationState, settings, teamCategories, stateWithNextStatesState, emailTemplatesState),
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
                    val icon = if (selected.contains(nomination.id)) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
                    clickableIcon(icon, "", surfaceColor) {
                        selected = if (selected.contains(nomination.id)) {
                            selected - nomination.id
                        } else {
                            selected + nomination.id
                        }
                    }
                    editButton { showEditor(nomination) }
                }
            }
        }
    }
}

@Composable
internal fun NominationEditor(nomination: Nomination, onDone: () -> Unit) {
    val viewModel: TrialViewModel = koinViewModel()
    val trialsState by viewModel.getState().collectAsState()
    val nominationViewModel: NominationViewModel = koinViewModel()
    val stateViewModel: StateViewModel = koinViewModel()
    val statesState by stateViewModel.getState().collectAsState()

    var firstName by remember { mutableStateOf(nomination.firstNameOfChild) }
    var surname by remember { mutableStateOf(nomination.surnameOfChild) }
    var birthDate by remember { mutableIntStateOf(nomination.childDateOfBirth) }
    var yearGroup by remember { mutableStateOf(nomination.whichYearGroupIsYourChildInNow) }
    var gender by remember { mutableStateOf(nomination.gender) }

    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun validate() : String? {
        if (firstName.isBlank() || surname.isBlank() || yearGroup.isBlank() || gender.isBlank() ||
            !validDateOfBirth(yearGroup, DayDate(birthDate).asUtcMs())) {
            return statesState.values().firstOrNull { StateType.entries[it.type] == StateType.ERROR }?.name
        }
        return statesState.values().firstOrNull { StateType.entries[it.type] == StateType.START }?.name
    }

    fun editorNomination(): Nomination = nomination.copy(
        firstNameOfChild = firstName,
        surnameOfChild = surname,
        whichYearGroupIsYourChildInNow = yearGroup,
        gender = gender,
        state = validate()
    )

    fun setEditorState() {
        val editorNomination = editorNomination()
        val stateLookup = statesState.values().associateBy { it.name }
        editorState = if (nomination == editorNomination) {
            EditorState.CLEAN
        } else if ((stateLookup[editorNomination.state]?.type ?: -1) == StateType.ERROR.ordinal) {
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
        title = "Edit Nomination: $firstName $surname",
        states = persistentListOf(trialsState),
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
        val readOnly = statesState.values()
            .filter { it.name == nomination.state }
            .map { StateType.entries[it.type].readOnly }
            .firstOrNull { it } ?: false
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(firstName, "First Name", readOnly) {
                firstName = it
                setEditorState()
            }
            gridEntry(surname, "Surname", readOnly) {
                surname = it
                setEditorState()
            }
            gridEntry(birthDate, "Date of Birth", readOnly,
                { validDateOfBirth(yearGroup, it)},
                {
                    birthDate = it
                    setEditorState()
                })
            gridEntry(yearGroup, "School Year", readOnly) {
                yearGroup = it
                setEditorState()
            }
            gridEntry(gender, "Gender", readOnly) {
                gender = it
                setEditorState()
            }

            viewTextItems(listOf("State", nomination.state ?: "", "Trials", ""))
        }
        TrailingIconLazyVerticalGrid(PaddingValues(), 3, 0) {
            viewTextItems(listOf("Date", "Attended", "Outcome"))
            trialsState.values().filter { it.nominee == nomination.id }.forEach { trial ->
                viewTextItems(listOf(DayDate(trial.date).toString()))
                gridEntry(trial.attended, enabled = false) {}
                viewTextItems(listOf(trial.outcome ?: ""))
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
