package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly

class SeasonWeeksTest : BehaviorSpec({
    given("a league competition finishing before the cup with a break") {
        val seasonCompetitions = listOf(
            createSeasonCompetition(1, "29/08/2025", "21/09/2025"), // League
            createSeasonCompetition(2, "15/09/2025", "05/10/2025")  // Cup
        )
        val breaks = listOf(SeasonBreak(1.toShort(), 1.toShort(), "Break 1", getDayDateVal("08/09/2025")))

        `when`("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(seasonCompetitions, breaks)

            Then("there should be a single break") {
                seasonWeeks.breakWeeks() shouldContainExactly mapOf(Pair(getDayDateVal("08/09/2025"), "Break 1"))
            }
            Then("the league season should exclude the break") {
                seasonWeeks.competitionWeeks(1.toShort()) shouldContainExactly listOf(getDayDateVal("01/09/2025"), getDayDateVal("15/09/2025"))
            }
            then("The cup competition should be consecutive weeks") {
                seasonWeeks.competitionWeeks(2.toShort()) shouldContainExactly listOf(getDayDateVal("15/09/2025"), getDayDateVal("22/09/2025"), getDayDateVal("29/09/2025"))
            }
        }
    }

    given("a cup competition with a start date of 0 and valid league competition") {
        val seasonCompetitions = listOf(
            createSeasonCompetition(1, "01/09/2025", "21/09/2025"), // League
            SeasonCompetition(1.toShort(), 2.toShort(), 0, getDayDateVal("05/10/2025"))
        )
        val breaks = emptyList<SeasonBreak>()

        `when`("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(seasonCompetitions, breaks)

            Then("breaks should be empty") {
                seasonWeeks.breakWeeks().shouldBeEmpty()
            }
            Then("the league season should exclude the break") {
                seasonWeeks.competitionWeeks(1.toShort()) shouldContainExactly listOf(getDayDateVal("01/09/2025"), getDayDateVal("08/09/2025"), getDayDateVal("15/09/2025"))
            }
        }
    }

    given("no valid competitions") {
        val breaks = listOf(SeasonBreak(1.toShort(), 1.toShort(), "Break 1", getDayDateVal("08/09/2025")))

        When("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(emptyList(), breaks)

            Then("breaks should be empty") {
                seasonWeeks.breakWeeks().shouldBeEmpty()
            }
        }
    }

//    given("a long season with several breaks") {
//        val seasonCompetitions = listOf(createSeasonCompetition(1, "08/09/2025", "05/04/2026"))
//        val breaks = listOf(createSeasonBreak(1, "27/10/2025"),
//            createSeasonBreak(2, "22/12/2025"),
//            createSeasonBreak(3, "29/12/2025"),
//            createSeasonBreak(4, "23/02/2026")
//        )
//        When("The season weeks are calculated") {
//            val seasonWeeks = SeasonWeeks(seasonCompetitions, breaks)
//
//            Then("Breaks should contain the 4 dates") {
//                seasonWeeks.breakWeeks() shouldContainExactly mapOf(Pair(getDayDateVal("27/10/2025"), "Break 1"),
//                    Pair(getDayDateVal("22/12/2025"), "Break 2"),
//                    Pair(getDayDateVal("29/12/2025"), "Break 3"),
//                    Pair(getDayDateVal("23/02/2026"), "Break 4"))
//            }
//
//            Then("the league season should exclude the breaks") {
//                var leagueWeeks = seasonWeeks.competitionWeeks(1.toShort())
//
//                leagueWeeks?.
//                shouldNotContain(getDayDateVal("27/10/2025"))?.
//                shouldNotContain(getDayDateVal("22/12/2025"))?.
//                shouldNotContain(getDayDateVal("29/12/2025"))?.
//                shouldNotContain(getDayDateVal("23/02/2026"))
//            }
//        }
//    }
})

private fun createSeasonBreak(id: Int, date: String): SeasonBreak = SeasonBreak(id.toShort(), 1.toShort(), "Break $id", getDayDateVal(date))

private fun createSeasonCompetition(
    competitionId: Int,
    startDate: String,
    endDate: String
): SeasonCompetition =
    SeasonCompetition(
        1.toShort(),
        competitionId.toShort(),
        getDayDateVal(startDate),
        getDayDateVal(endDate)
    )

private fun getDayDateVal(date: String): Int {
    return DayDate(date).value()
}
