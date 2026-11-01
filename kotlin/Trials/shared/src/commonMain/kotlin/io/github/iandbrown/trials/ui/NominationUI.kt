package io.github.iandbrown.trials.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.iandbrown.trials.database.Nomination
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.database.Setting
import io.github.iandbrown.trials.database.State
import io.github.iandbrown.trials.database.StateType
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

internal class NominationViewModel(dao: NominationDao) : CRUDViewModel<NominationDao, Nomination>(dao = dao), KoinComponent {
    internal fun sendEmail(nomination: Nomination, settings: Setting?) {
        if (settings != null) {
            runInCoroutine {
                val emailSender: EmailSender = get { parametersOf(settings) }
                emailSender.send(
                    from = settings.userName,
                    to = nomination.emailAddress,
                    subject = "Nomination for ${nomination.firstNameOfChild} ${nomination.surnameOfChild}",
                    body = "Nomination details for ${nomination.firstNameOfChild} ${nomination.surnameOfChild}"
                )
            }
        }
    }
}

internal class TrialViewModel(dao : TrialDao) : CRUDViewModel<TrialDao, Trial>(dao = dao), KoinComponent

@Composable
internal fun NominationList(showEditor: (Nomination) -> Unit) {
    val viewModel: NominationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val settingsModel: SettingsViewModel = koinViewModel()
    val settings by settingsModel.getState().collectAsState()
    val stateViewModel: StateViewModel = koinViewModel()
    val states by stateViewModel.getState().collectAsState()
    val teamCategoryViewModel: TeamCategoryViewModel = koinViewModel()
    val teamCategories by teamCategoryViewModel.getState().collectAsState()
    val startState = states.values().firstOrNull { StateType.entries[it.type] == StateType.START }
    val errorState = states.values().firstOrNull { StateType.entries[it.type] == StateType.ERROR }
    var teamCategoryFilter by remember { mutableIntStateOf(0) }
    var stateFilter by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    ViewCommon("Nominations",
        persistentListOf(state, settings, teamCategories),
        bottomBar = {
            BottomBarWithButtons(
                importButtonSettings(viewModel) { row ->
                    toNomination(row, startState, errorState)
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
                val stateList = (listOf("") + states.values().map { it.name }).toImmutableList()
                DropdownList(stateList, stateFilter, modifier = Modifier.weight(1f)) {
                    stateFilter = it
                    selected = setOf()
                }
                Spacer(modifier = Modifier.size(8.dp))
                ViewText("Selected: ${selected.size}", Modifier.weight(1f))
            }

            val (columnCount, header) = if (teamCategoryFilter == 0) {
                Pair(5,listOf("Name", "School year", "Gender", "Primary", "Secondary", "", "", ""))
            } else {
                Pair(3,listOf("Name", "Primary", "Secondary", "", "", ""))
            }
            val nominationValues = state.values()
                .filter { teamCategoryFilter == 0 ||
                        it.gender == teamCategories.values()[teamCategoryFilter - 1].gender.displayName &&
                        it.whichYearGroupIsYourChildInNow == teamCategories.values()[teamCategoryFilter - 1].yearGroup.displayName }
                .filter { stateFilter == 0 || it.state == states.values()[stateFilter - 1].name }
            val surfaceColor = MaterialTheme.colorScheme.onSurface

            TrailingIconLazyVerticalGrid(PaddingValues(), columnCount, 3) {
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
                    clickableIcon(Icons.Default.Email, "mail", Color.Green) {
                        viewModel.sendEmail(nomination, settings.values().firstOrNull())
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

internal fun toNomination(row: DataRow<Any?>, startState: State?, errorState: State?): Nomination {
    val childBirthDate = date(row[4])
    val yearGroup = string(row[7])
    val state = when (validDateOfBirth(yearGroup, DayDate(childBirthDate).asUtcMs())) {
        true -> startState?.name
        else -> errorState?.name
    }
    if (errorState != null && state == errorState.name) {
        println("${string(row[2])} ${string(row[3])} ${row[4]} $yearGroup ${DayDate(childBirthDate)}")
    }

    val adultDetails = io.github.iandbrown.trials.database.AdultDetails(
        type = io.github.iandbrown.trials.database.AdultType.PARENT_OR_CARER,
        emailAddress = string(row[1]),
        firstNameOfParentOrCarer = string(row[14]),
        surnameOfParentOrCarer = string(row[15]),
        parentOrCarerMobileNumber = string(row[16]),
        parentOrCarerEmail = string(row[17]),
        confirmEmail = string(row[18])
    )
    return Nomination(
        state,
        adultDetails = adultDetails,
        firstNameOfChild = string(row[2]),
        surnameOfChild = string(row[3]),
        childDateOfBirth = childBirthDate,
        gender = string(row[5]),
        whichSchoolDoesYourChildAttend = string(row[6]),
        whichYearGroupIsYourChildInNow = yearGroup,
        primaryPosition = string(row[8]),
        secondaryPosition = string(row[9]),
        foot = string(row[10]),
        doesYourChildPlayForATeamOnTheWeekend = string(row[11]),
        whatTypeOfTeamDoTheyPlayFor = string(row[12]),
        teamTheyPlayFor = string(row[13]),
        medicalConditions = string(row[19]),
        iWishMyChildToAttendTrials = string(row[20]),
        consentToTheTakingOfPhotographs = string(row[21])
    )
}

internal fun string(cell: Any?): String =
    when (cell) {
        is String -> cell
        is Double -> cell.toString()
        is Char -> cell.toString()
        else -> ""
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
