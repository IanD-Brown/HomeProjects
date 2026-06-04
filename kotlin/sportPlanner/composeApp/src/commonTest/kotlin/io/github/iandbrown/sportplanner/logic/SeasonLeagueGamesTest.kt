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
            seasonLeagueGames.prepareGames(1, 1, 1, teams)
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
            seasonLeagueGames.prepareGames(1, 1, 2, teams)
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
            seasonLeagueGames.prepareGames(1, 1, 0, teams)
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
