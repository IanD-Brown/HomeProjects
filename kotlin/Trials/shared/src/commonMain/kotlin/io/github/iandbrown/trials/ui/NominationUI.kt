package io.github.iandbrown.trials.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.iandbrown.trials.database.Nomination
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.logic.DayDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import org.jetbrains.kotlinx.dataframe.DataRow
import org.koin.compose.viewmodel.koinViewModel
import java.math.BigDecimal

internal class NominationViewModel(dao: NominationDao) : CRUDViewModel<NominationDao, Nomination>(dao = dao)

@Composable
internal fun NominationList() {
    val viewModel: NominationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()

    ViewCommon("Nominations",
        persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                importButtonSettings(viewModel) { row ->
                    toNomination(row)
                }
            )
        }) { padding ->
        TrailingIconLazyVerticalGrid(padding, 4, 0) {
            viewTextItems(listOf("Email Address", "First Name", "Surname", "Date of Birth"))
            state.values().forEach {
                viewTextItems(listOf(it.emailAddress, it.firstNameOfChild, it.surnameOfChild, DayDate(it.childDateOfBirth).toString()))
            }
        }
    }
}

internal fun toNomination(row: DataRow<Any?>): Nomination {
    val childBirthDate = date(row[4])
    if (childBirthDate == 0) {
        println(row)
    }
    return Nomination(
        string(row[1]),
        string(row[2]),
        string(row[3]),
        childBirthDate,
        string(row[5]),
        string(row[6]),
        string(row[7]),
        string(row[8]),
        string(row[9]),
        string(row[10]),
        string(row[11]),
        string(row[12]),
        string(row[13]),
        string(row[14]),
        string(row[15]),
        string(row[16]),
        string(row[17]),
        string(row[18]),
        string(row[19]),
        string(row[20]),
        string(row[21])
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
