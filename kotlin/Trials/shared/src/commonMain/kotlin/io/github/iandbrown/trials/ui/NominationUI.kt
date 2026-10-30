package io.github.iandbrown.trials.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.trials.database.Nomination
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.database.Setting
import io.github.iandbrown.trials.database.StateDao
import io.github.iandbrown.trials.database.Trial
import io.github.iandbrown.trials.database.TrialDao
import io.github.iandbrown.trials.logic.DayDate
import io.github.iandbrown.trials.logic.EmailSender
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import org.jetbrains.kotlinx.dataframe.DataRow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal

internal class NominationViewModel(
    dao: NominationDao,
    val trialDao: TrialDao,
    val stateDao: StateDao
) : CRUDViewModel<NominationDao, Nomination>(dao = dao), KoinComponent {

    private val trialDelegate = ReadDelegate(viewModelScope) { trialDao.get() }
    val trialState = trialDelegate.uiState

    private val stateDelegate = ReadDelegate(viewModelScope) { stateDao.get() }
    val statesState = stateDelegate.uiState

    fun addTrial(trial: Trial) {
        runInCoroutine { trialDao.insert(trial) }
        trialDelegate.readAll()
    }

    fun updateTrial(trial: Trial) {
        runInCoroutine { trialDao.update(trial) }
        trialDelegate.readAll()
    }

    fun deleteTrial(trial: Trial) {
        runInCoroutine { trialDao.delete(trial) }
        trialDelegate.readAll()
    }

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

@Composable
internal fun NominationList() {
    val viewModel: NominationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    val settingsModel: SettingsViewModel = koinViewModel()
    val settings by settingsModel.getState().collectAsState()

    var editingNomination by remember { mutableStateOf<Nomination?>(null) }

    if (editingNomination != null) {
        NominationEditor(
            nomination = editingNomination!!,
            viewModel = viewModel,
            onDone = { editingNomination = null }
        )
    } else {
        ViewCommon("Nominations",
            persistentListOf(state, settings),
            bottomBar = {
                BottomBarWithButtons(
                    importButtonSettings(viewModel) { row ->
                        toNomination(row)
                    }
                )
            }) { padding ->
            val sortedNominations = state.values()
            TrailingIconLazyVerticalGrid(padding, 5, 2) {
                viewTextItems(listOf("Name", "School year", "Gender", "Primary", "Secondary", "", ""))
                sortedNominations.forEach { nomination ->
                    viewTextItems(listOf("${nomination.firstNameOfChild} ${nomination.surnameOfChild.trim()}",
                        nomination.whichYearGroupIsYourChildInNow,
                        nomination.gender,
                        nomination.primaryPosition,
                        nomination.secondaryPosition))
                    clickableIcon(Icons.Default.Email, "mail", Color.Green) {
                        viewModel.sendEmail(nomination, settings.values().firstOrNull())
                    }
                    editButton { editingNomination = nomination }
                }
            }
        }
    }
}

@Composable
internal fun NominationEditor(
    nomination: Nomination,
    viewModel: NominationViewModel,
    onDone: () -> Unit
) {
    val trialsState by viewModel.trialState.collectAsState()
    val statesState by viewModel.statesState.collectAsState()

    var firstName by remember { mutableStateOf(nomination.firstNameOfChild) }
    var surname by remember { mutableStateOf(nomination.surnameOfChild) }
    var yearGroup by remember { mutableStateOf(nomination.whichYearGroupIsYourChildInNow) }
    var gender by remember { mutableStateOf(nomination.gender) }
    var selectedState by remember { mutableStateOf(nomination.state ?: "") }

    var newTrialDateString by remember { mutableStateOf("") }
    var newTrialAttended by remember { mutableStateOf(false) }
    var newTrialOutcome by remember { mutableStateOf("") }

    val availableStates = statesState.values().map { it.name }
    val stateOptions = (listOf("None") + availableStates).toImmutableList()
    val selectedStateIndex = if (selectedState.isBlank()) 0 else stateOptions.indexOf(selectedState).let { if (it < 0) 0 else it }

    val relatedTrials = trialsState.values().filter { it.nominee == nomination.id }

    fun saveNomination() {
        val stateRef = if (selectedState.isBlank() || selectedState == "None") null else selectedState
        val updated = nomination.copy(
            firstNameOfChild = firstName,
            surnameOfChild = surname,
            whichYearGroupIsYourChildInNow = yearGroup,
            gender = gender,
            state = stateRef
        )
        viewModel.update(updated)
        onDone()
    }

    ViewCommon(
        title = "Edit Nomination: $firstName $surname",
        description = "Return to Nomination List",
        bottomBar = {
            BottomBarWithButton {
                saveNomination()
            }
        },
        confirm = { true },
        confirmAction = { saveNomination() }
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(firstName, "First Name") { firstName = it }
            gridEntry(surname, "Surname") { surname = it }
            gridEntry(yearGroup, "School Year") { yearGroup = it }
            gridEntry(gender, "Gender") { gender = it }

            item { ViewText("State") }
            item {
                DropdownList(
                    itemList = stateOptions,
                    selectedIndex = selectedStateIndex,
                    onItemClick = { idx ->
                        selectedState = if (idx == 0) "" else stateOptions[idx]
                    }
                )
            }

            item { ViewText("Related Trials") }
            item { ViewText("") }

            item { ViewText("Date") }
            item { ViewText("Attended / Outcome") }

            relatedTrials.forEach { trial ->
                val dateDisplay = DayDate(trial.date).toString()
                item { ViewText(dateDisplay) }
                item {
                    val outcomeDisplay = trial.outcome ?: "No Outcome"
                    Row {
                        OutlinedTextButton(
                            value = "${if (trial.attended) "Attended" else "Absent"} | $outcomeDisplay",
                            onClick = {
                                viewModel.updateTrial(trial.copy(attended = !trial.attended))
                            }
                        )
                        IconButton(onClick = { viewModel.deleteTrial(trial) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Trial", tint = Color.Red)
                        }
                    }
                }
            }

            item { ViewText("Add Trial (dd/mm/yyyy)") }
            item {
                ViewTextField(
                    value = newTrialDateString,
                    onValueChange = { newTrialDateString = it },
                    label = "Date"
                )
            }

            item { ViewText("New Trial Outcome") }
            item {
                DropdownList(
                    itemList = stateOptions,
                    selectedIndex = if (newTrialOutcome.isBlank()) 0 else stateOptions.indexOf(newTrialOutcome).let { if (it < 0) 0 else it },
                    onItemClick = { idx ->
                        newTrialOutcome = if (idx == 0) "" else stateOptions[idx]
                    }
                )
            }

            item {
                OutlinedTextButton(
                    value = "Add Trial",
                    enabled = newTrialDateString.isNotBlank(),
                    onClick = {
                        val parsedDate = DayDate(newTrialDateString).value()
                        val outcomeRef = if (newTrialOutcome.isBlank() || newTrialOutcome == "None") null else newTrialOutcome
                        viewModel.addTrial(
                            Trial(
                                nominee = nomination.id,
                                date = parsedDate,
                                attended = newTrialAttended,
                                outcome = outcomeRef
                            )
                        )
                        newTrialDateString = ""
                        newTrialOutcome = ""
                    }
                )
            }
            item { ViewText("") }
        }
    }
}

internal fun toNomination(row: DataRow<Any?>): Nomination {
    val childBirthDate = date(row[4])
    if (childBirthDate == 0) {
        println(row)
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
        adultDetails = adultDetails,
        firstNameOfChild = string(row[2]),
        surnameOfChild = string(row[3]),
        childDateOfBirth = childBirthDate,
        gender = string(row[5]),
        whichSchoolDoesYourChildAttend = string(row[6]),
        whichYearGroupIsYourChildInNow = string(row[7]),
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
    SHORT_DIGIT("""(\d{2})(\d{2})(\d{2})([.\d]*)""".toRegex(), 3, 2, 1),
    LONG_DIGIT("""(\d{2})(\d{2})(\d{4})([.\d]*)""".toRegex(), 3, 2, 1);

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
    return DateType.entries.find { it.matches(dateString) }?.dateIndex(dateString) ?: 0
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
