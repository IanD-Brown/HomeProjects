package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.ui.CompetitionTypes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.text.SimpleDateFormat

class SeasonWeeksTest : BehaviorSpec({

    given("a season with two competitions and one break") {
        val seasonCompetitions = listOf(
            SeasonCompetition(1.toShort(), 1.toShort(), getMsTime("01/09/2025"), getMsTime("21/09/2025")),
            SeasonCompetition(1.toShort(), 2.toShort(), getMsTime("15/09/2025"), getMsTime("05/10/2025"))
        )
        val breaks = listOf(
            SeasonBreak(1.toShort(), 1.toShort(), "Break 1", getMsTime("08/09/2025"))
        )
        val competitions = listOf(
            Competition(1.toShort(), "Comp 1", CompetitionTypes.LEAGUE.ordinal.toShort()),
            Competition(2.toShort(), "Comp 2", CompetitionTypes.KNOCK_OUT_CUP.ordinal.toShort())
        )

        `when`("the season weeks are calculated") {
            val seasonWeeks = SeasonWeeks(seasonCompetitions, breaks, competitions)

            then("the number of league playing weeks should be 2") {
                seasonWeeks.leaguePlayingWeeks() shouldBe 2
            }

            then("the weeks should be visited correctly") {
                val weeks = mutableMapOf<Long, Pair<Boolean, String?>>()
                seasonWeeks.visitWeeks { monday, message, league ->
                    weeks[monday] = Pair(league, message)
                }

                weeks.shouldContainExactly(mapOf(
                    getMsTime("01/09/2025") to Pair(true, null),
                    getMsTime("08/09/2025") to Pair(true, "Break 1"),
                    getMsTime("15/09/2025") to Pair(true, null),
                    getMsTime("22/09/2025") to Pair(false, null),
                    getMsTime("29/09/2025") to Pair(false, null))
                )
            }
        }
    }
})

private fun getMsTime(date : String): Long =
    SimpleDateFormat("dd/MM/yyyy").parse(date)?.time!!
