package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.ui.CompetitionTypes
import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.text.SimpleDateFormat

class SeasonWeeksTest : BehaviorSpec({
    val leagueCompetitionRef = CompetitionRef(1.toShort(), "Comp 1", CompetitionTypes.LEAGUE)
    val cupCompetitionRef = CompetitionRef(2.toShort(), "Comp 2", CompetitionTypes.KNOCK_OUT_CUP)

    given("a league competition finishing before the cup with a break") {
        val seasonCompetitions = listOf(
            createSeasonCompetition(1, "29/08/2025", "21/09/2025"), // League
            createSeasonCompetition(2, "15/09/2025", "05/10/2025")  // Cup
        )
        val breaks = listOf(SeasonBreak(1.toShort(), 1.toShort(), "Break 1", getMsTime("08/09/2025")))
        val competitions = createTwoCompetition()

        `when`("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(seasonCompetitions, breaks, competitions)

            then("the number of league playing weeks should be 2") {
                seasonWeeks.leaguePlayingWeeks() shouldBe 2
            }

            then("the weeks should be visited correctly with active competitions") {
                seasonWeeks.visitWeeks { monday, message -> 
                    when (monday) {
                        getMsTime("01/09/2025") -> {
                            // Week 1: League only
                            message shouldBe null
                            seasonWeeks.getActiveCompetitions(monday) shouldContainExactlyInAnyOrder listOf(leagueCompetitionRef)
                        }
                        getMsTime("08/09/2025") -> {
                            // Week 2: League only, with a break
                            message shouldBe "Break 1"
                            seasonWeeks.getActiveCompetitions(monday) shouldContainExactlyInAnyOrder listOf(
                                leagueCompetitionRef
                            )
                        }
                        getMsTime("15/09/2025") -> {
                            // Week 3: League and Cup
                            message shouldBe null
                            seasonWeeks.getActiveCompetitions(monday) shouldContainExactlyInAnyOrder listOf(
                                leagueCompetitionRef,
                                cupCompetitionRef
                            )
                        }
                        getMsTime("22/09/2025") -> {
                            // Week 4: Cup only
                            message shouldBe null
                            seasonWeeks.getActiveCompetitions(monday) shouldContainExactlyInAnyOrder listOf(
                                cupCompetitionRef
                            )
                        }
                        getMsTime("29/09/2025") -> {
                            // Week 5: Cup only
                            message shouldBe null
                            seasonWeeks.getActiveCompetitions(monday) shouldContainExactlyInAnyOrder listOf(
                                cupCompetitionRef
                            )
                        }
                        else -> {
                            fail("Didn't expect $monday")
                        }
                    }
                }
            }
        }
    }

    given("a cup competition with a start date of 0 and valid league competition") {
        val seasonCompetitions = listOf(
            createSeasonCompetition(1, "01/09/2025", "21/09/2025"), // League
            SeasonCompetition(1.toShort(), 2.toShort(), 0, getMsTime("05/10/2025"))
        )
        val breaks = emptyList<SeasonBreak>()
        val competitions = createTwoCompetition()

        `when`("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(seasonCompetitions, breaks, competitions)

            then("the number of league playing weeks should be 3") {
                seasonWeeks.leaguePlayingWeeks() shouldBe 3
            }

            then("the weeks should be visited correctly, ignoring the invalid cup date") {
                seasonWeeks.visitWeeks { monday, message ->
                    when (monday) {
                        getMsTime("01/09/2025") -> {}
                        getMsTime("08/09/2025") -> {}
                        getMsTime("15/09/2025") -> {}
                        else -> {
                            fail("Didn't expect $monday")
                        }
                    }
                    message shouldBe null
                    seasonWeeks.getActiveCompetitions(monday).map {it.id}.shouldContainExactlyInAnyOrder(listOf(1.toShort()))
                }
            }
        }
    }

    given("no valid competitions") {
        When("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(emptyList(), emptyList(), emptyList())

            then("the number of league playing weeks should be 0") {
                seasonWeeks.leaguePlayingWeeks() shouldBe 0L
                seasonWeeks.visitWeeks { _, _ -> fail("No valid weeks")}
            }
        }
    }
})

private fun createSeasonCompetition(
    competitionId: Int,
    startDate: String,
    endDate: String
): SeasonCompetition =
    SeasonCompetition(
        1.toShort(),
        competitionId.toShort(),
        getMsTime(startDate),
        getMsTime(endDate)
    )

private fun createTwoCompetition(): List<Competition> = listOf(
    Competition(1.toShort(), "Comp 1", CompetitionTypes.LEAGUE.ordinal.toShort()),
    Competition(2.toShort(), "Comp 2", CompetitionTypes.KNOCK_OUT_CUP.ordinal.toShort())
)

private fun getMsTime(date: String): Long {
    if (date.isBlank()) return 0
    return SimpleDateFormat("dd/MM/yyyy").parse(date).time
}
