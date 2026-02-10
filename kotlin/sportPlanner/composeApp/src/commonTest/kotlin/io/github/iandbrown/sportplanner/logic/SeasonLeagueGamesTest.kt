package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.SeasonCompRoundView
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class SeasonLeagueGamesTest : BehaviorSpec({
    given("a set of teams for a league") {
        val teams = listOf(
            SeasonTeam(1, 1, 1, 1, 2),
            SeasonTeam(1, 1, 2, 1, 1)
        )
        val seasonLeagueGames = SeasonLeagueGames()

        `when`("a single game structure is prepared") {
            seasonLeagueGames.prepareGames(1, 1, 1, teams)
            val plannedGames = seasonLeagueGames.getPlannedGames(1)
            val expandedTeams = buildAllTeams(teams)

            then("each team should play against every other team once") {
                for (team in expandedTeams) {
                    val opponents = plannedGames.map {
                        if (it.homeAssociationId == team.first && it.homeTeamNumber == team.second)
                            Pair(it.awayAssociationId, it.awayTeamNumber)
                        else
                            Pair(it.homeAssociationId, it.homeTeamNumber)
                    }.toSet()
                    opponents.size shouldBe 2
                }
            }

            then("the home and away game count for each team should be the same") {
                val gameCounts = mutableMapOf<Pair<Short, Short>, Pair<Int, Int>>()
                for (team in expandedTeams) {
                    for (plannedGame in plannedGames) {
                        val isHome = plannedGame.homeAssociationId == team.first && plannedGame.homeTeamNumber == team.second

                        gameCounts.getOrPut(team) { Pair(0, 0) }.let {
                            gameCounts[team] = Pair(it.first + if (isHome) 1 else 0, it.second + if (isHome) 0 else 1)
                        }
                    }
                }
                for (gameCount in gameCounts.values) {
                    for (gameCount2 in gameCounts.values) {
                        gameCount.first shouldBe gameCount2.first
                        gameCount.second shouldBe gameCount2.second
                    }
                }
            }
        }

        `when`("a home and away game structure is prepared") {
            seasonLeagueGames.prepareGames(1, 1, 2, teams)
            val plannedGames = seasonLeagueGames.getPlannedGames(1)

            then("the correct number of games should be created") {
                plannedGames.size shouldBe 6
            }

            then("the games should be correct") {
                plannedGames shouldContainExactlyInAnyOrder listOf(
                    PlannedGame(1, 1, 1, 1, 2),
                    PlannedGame(1, 1, 2, 1, 1),
                    PlannedGame(1, 1, 1, 2, 1),
                    PlannedGame(1, 2, 1, 1, 1),
                    PlannedGame(1, 1, 2, 2, 1),
                    PlannedGame(1, 2, 1, 1, 2)
                )
            }
        }

        `when`("an unsupported game structure") {
            seasonLeagueGames.prepareGames(1, 1, 0, teams)
            val plannedGames = seasonLeagueGames.getPlannedGames(1)

            then("no games should be created") {
                plannedGames shouldBe emptyList()
            }
        }

        `when`("the fixtures are scheduled") {
            val seasonCompetitions = listOf(
                createSeasonCompView(1, "01/09/2025", "24/11/2025")
            )
            val seasonWeeks = SeasonWeeksImpl(seasonCompetitions, emptyList())
            val competitionRounds = listOf(
                SeasonCompRoundView(1, "Round 1", getDayDateVal("08/09/2025"), false, 1)
            )
            val seasonTeamCategories = listOf(
                SeasonTeamCategory(1, 1, 1, 2, false)
            )
            val teamCategories = listOf(TeamCategory(1, "Team category", 1))

            seasonLeagueGames.prepareGames(1, 1, 2, teams)
            val scheduledFixtures = seasonLeagueGames.scheduleFixtures(
                1,
                seasonWeeks,
                teamCategories,
                seasonTeamCategories,
                competitionRounds,
                mapOf(Pair(Pair(1, 1), 3)),
                setOf(1.toShort())
            )

            then("the correct number of fixtures should be created") {
                scheduledFixtures.size shouldBe 7
            }

            then("no fixtures should be scheduled on the cup week") {
                scheduledFixtures.none { it.date == getDayDateVal("08/09/2025") && (it.homeTeamNumber > 0 || it.awayTeamNumber > 0) } shouldBe true
            }
        }

        When("the season is too short for the games to be scheduled") {
            val seasonCompetitions = listOf(
                createSeasonCompView(1, "01/09/2025", "07/09/2025")
            )
            val seasonWeeks = SeasonWeeksImpl(seasonCompetitions, emptyList())
            val seasonTeamCategories = listOf(
                SeasonTeamCategory(1, 1, 1, 2, false)
            )
            val teamCategories = listOf(TeamCategory(1, "Team category", 1))

            seasonLeagueGames.prepareGames(1, 1, 2, teams)
            val scheduledFixtures = seasonLeagueGames.scheduleFixtures(
                1,
                seasonWeeks,
                teamCategories,
                seasonTeamCategories,
                emptyList(),
                mapOf(Pair(Pair(1, 1), 3)),
                setOf(1.toShort())
            )

            then("a fixture should have the message INCOMPLETE") {
                scheduledFixtures.filter{it.message == "INCOMPLETE"}.size shouldBe 1
            }
        }
    }
})

private fun buildAllTeams(teams: List<SeasonTeam>): List<Pair<Short, Short>> = teams.flatMap { seasonTeam ->
    (1..seasonTeam.count).map { teamNumber ->
        Pair(seasonTeam.associationId, teamNumber.toShort())
    }
}

private fun createSeasonCompView(competitionId: Int, startDate: String, endDate: String): SeasonCompView =
    SeasonCompView(
        1.toShort(),
        "",
        competitionId.toShort(),
        "",
        0.toShort(),
        getDayDateVal(startDate),
        getDayDateVal(endDate)
    )

private fun getDayDateVal(date: String): Int {
    return DayDate(date).value()
}
