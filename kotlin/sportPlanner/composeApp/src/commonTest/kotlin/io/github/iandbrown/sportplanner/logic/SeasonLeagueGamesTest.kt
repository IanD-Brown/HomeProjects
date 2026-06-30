package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonCompRoundView
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamNumber
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

        When("a single game structure is prepared") {
            seasonLeagueGames.prepareGames(1, 1, 1, teams, emptyMap())
            val plannedGames = seasonLeagueGames.getPlannedGames(1)
            val expandedTeams = buildAllTeams(teams)

            then("each team should play against every other team once") {
                for (team in expandedTeams) {
                    val opponents = plannedGames.map {
                        if (it.home.associationId == team.first && it.home.teamNumber == team.second)
                            Pair(it.away.associationId, it.away.teamNumber)
                        else
                            Pair(it.home.associationId, it.home.teamNumber)
                    }.toSet()
                    opponents.size shouldBe 2
                }
            }

            then("the home and away game count for each team should be the same") {
                val gameCounts = mutableMapOf<Pair<Short, Short>, Pair<Int, Int>>()
                for (team in expandedTeams) {
                    for (plannedGame in plannedGames) {
                        val isHome = plannedGame.home.associationId == team.first && plannedGame.home.teamNumber == team.second

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

        When("a home and away game structure is prepared") {
            seasonLeagueGames.prepareGames(1, 1, 2, teams, emptyMap())
            val plannedGames = seasonLeagueGames.getPlannedGames(1)

            then("the correct number of games should be created") {
                plannedGames.size shouldBe 6
            }

            then("the games should be correct") {
                plannedGames shouldContainExactlyInAnyOrder listOf(
                    plannedGameOf(1, 1, 1, 1, 2),
                    plannedGameOf(1, 1, 2, 1, 1),
                    plannedGameOf(1, 1, 1, 2, 1),
                    plannedGameOf(1, 2, 1, 1, 1),
                    plannedGameOf(1, 1, 2, 2, 1),
                    plannedGameOf(1, 2, 1, 1, 2)
                )
            }
        }

        When("an unsupported game structure") {
            seasonLeagueGames.prepareGames(1, 1, 0, teams, emptyMap())
            val plannedGames = seasonLeagueGames.getPlannedGames(1)

            then("no games should be created") {
                plannedGames shouldBe emptyList()
            }
        }

        When("the fixtures are scheduled") {
            val seasonCompetitions = listOf(
                createSeasonCompView(1, "01/09/2025", "24/11/2025")
            )
            val seasonWeeks = SeasonWeeksImpl(seasonCompetitions, emptyList())
            val competitionRounds = listOf(
                SeasonCompRoundView(1, "Round 1", getDayDateVal("08/09/2025"), false, 1, "")
            )
            val seasonTeamCategories = listOf(
                SeasonTeamCategory(1, 1, 1, 2, false)
            )
            val teamCategories = listOf(TeamCategory(1, "Team category", 1))

            seasonLeagueGames.prepareGames(1, 1, 2, teams, emptyMap())
            val scheduledFixtures = seasonLeagueGames.scheduleFixtures(
                1,
                seasonWeeks,
                teamCategories,
                seasonTeamCategories,
                competitionRounds,
                mapOf(Pair(Pair(1, 1), 3)),
                setOf(1.toShort()),
                emptyMap()
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

            seasonLeagueGames.prepareGames(1, 1, 2, teams, emptyMap())
            val scheduledFixtures = seasonLeagueGames.scheduleFixtures(
                1,
                seasonWeeks,
                teamCategories,
                seasonTeamCategories,
                emptyList(),
                mapOf(Pair(Pair(1, 1), 3)),
                setOf(1.toShort()),
                emptyMap()
            )

            then("a fixture should have the message INCOMPLETE") {
                scheduledFixtures.filter{it.message == "INCOMPLETE"}.size shouldBe 1
            }
        }
    }
    given("a number of games to be planned") {
        val side1 = Side(1, 1, 1)
        val side2 = Side(1, 2, 1)
        val side3 = Side(1, 3, 1)
        val side4 = Side(1, 4, 1)
        val plannedGames = listOf(
            PlannedGame(1, home = side1, away = side2),
            PlannedGame(1, home = side4, away = side3),
            PlannedGame(1, home = side3, away = side1, distantAwayGame = true)
        )
        When("the games to schedule and home game count match") {
            val gamesToSchedule = mutableMapOf<Side, Int>()
            val homeGameByAssociation = mutableMapOf<AssociationId, Int>()

            gamesToSchedule[side1] = 0
            gamesToSchedule[side2] = 4
            gamesToSchedule[side3] = 0
            gamesToSchedule[side4] = 0
            homeGameByAssociation[side1.associationId] = 2
            homeGameByAssociation[side2.associationId] = 2
            homeGameByAssociation[side3.associationId] = 2
            homeGameByAssociation[side4.associationId] = 2

            val orderedGames = getOrderedGames(plannedGames, gamesToSchedule, homeGameByAssociation)

            then("distant away games are earlier") {
                orderedGames[0] shouldBe plannedGames[0]
                orderedGames[1] shouldBe plannedGames[2]
                orderedGames[2] shouldBe plannedGames[1]
            }
        }
        When("the games to schedule are the same") {
            val gamesToSchedule = mutableMapOf<Side, Int>()
            val homeGameByAssociation = mutableMapOf<AssociationId, Int>()

            gamesToSchedule[side1] = 0
            gamesToSchedule[side2] = 4
            gamesToSchedule[side3] = 0
            gamesToSchedule[side4] = 0
            homeGameByAssociation[side1.associationId] = 2
            homeGameByAssociation[side2.associationId] = 2
            homeGameByAssociation[side3.associationId] = 2
            homeGameByAssociation[side4.associationId] = 0

            val orderedGames = getOrderedGames(plannedGames, gamesToSchedule, homeGameByAssociation)

            then("the association having the least home games should be earlier") {
                orderedGames[0] shouldBe plannedGames[0]
                orderedGames[1] shouldBe plannedGames[1]
                orderedGames[2] shouldBe plannedGames[2]
            }
        }

        When("the games to schedule are the different") {
            val gamesToSchedule = mutableMapOf<Side, Int>()
            val homeGameByAssociation = mutableMapOf<AssociationId, Int>()

            gamesToSchedule[side1] = 2
            gamesToSchedule[side2] = 1
            gamesToSchedule[side3] = 3
            gamesToSchedule[side4] = 4
            homeGameByAssociation[side1.associationId] = 2
            homeGameByAssociation[side2.associationId] = 2
            homeGameByAssociation[side3.associationId] = 2
            homeGameByAssociation[side4.associationId] = 2

            val orderedGames = getOrderedGames(plannedGames, gamesToSchedule, homeGameByAssociation)

            then("the game with the higher games to schedule should be earlier") {
                orderedGames[0] shouldBe plannedGames[1]
                orderedGames[1] shouldBe plannedGames[2]
                orderedGames[2] shouldBe plannedGames[0]
            }
        }
    }
})

private fun plannedGameOf(compId: CompetitionId,
                          homeAssoc: AssociationId,
                          homeNumber: TeamNumber,
                          awayAssoc: AssociationId,
                          awayNumber: TeamNumber) : PlannedGame =
    PlannedGame(compId, Side(1, homeAssoc, homeNumber), Side(1, awayAssoc, awayNumber))

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
