package io.github.iandbrown.sportplanner.ui

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.MokkeryMatcherScope
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.mock
import dev.mokkery.verifyNoMoreCalls
import dev.mokkery.verifySuspend
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonCompRoundViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonFixtureDao
import io.github.iandbrown.sportplanner.database.SeasonFixtureView
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.database.TeamNumber
import io.github.iandbrown.sportplanner.logic.DayDate
import io.github.iandbrown.sportplanner.logic.SeasonWeeks
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private const val seasonId = 1.toShort()
private const val associationId = 2.toShort()
private const val teamCategoryId = 20.toShort()
private const val secondTeamCategoryId = 21.toShort()

class SeasonFixtureViewTest : BehaviorSpec({
    val competitionId = 10.toShort()

    val seasonFixtureDao = mock<SeasonFixtureDao>()
    val seasonTeamDao = mock<SeasonTeamDao>()
    val seasonCompetitionDao = mock<SeasonCompetitionDao>()
    val seasonTeamCategoryDao = mock<SeasonTeamCategoryDao>()
    val teamCategoryDao = mock<TeamCategoryDao>()
    val seasonCompRoundViewDao = mock<SeasonCompRoundViewDao>()
    val seasonWeeks = mock<SeasonWeeks>()

    given("a season with one active competition and team category") {
        val activeCompetitions = listOf(seasonCompetitionOf(competitionId))
        val teamCategories = listOf(TeamCategory(teamCategoryId, "Name", 6.toShort()))

        everySuspend { seasonCompetitionDao.getActiveLeagueCompetitions(seasonId) } returns activeCompetitions
        everySuspend {
            seasonFixtureDao.deleteBySeasonTeamCategory(seasonId, teamCategoryId, competitionId)
        } returns Unit
        everySuspend { teamCategoryDao.getAsList() } returns teamCategories
        everySuspend { seasonCompRoundViewDao.getBySeason(seasonId) } returns emptyList()
        everySuspend { seasonFixtureDao.insert(any()) } returns 0L
        everySuspend {
            seasonFixtureDao.deleteBySeasonTeamCategory(any(), any(), any())
        } returns Unit
        every { seasonWeeks.competitionWeeks(competitionId) } returns listOf(
            DayDate("01/01/2025").value(),
            DayDate("08/01/2025").value(),
            DayDate("15/01/2025").value(),
            DayDate("22/01/2025").value())
        every { seasonWeeks.competitions() } returns listOf(competitionId)

        When("calcFixtures is called with empty breaks") {
            setSeasonCompTeams(seasonTeamCategoryDao, seasonTeamDao, competitionId, teamCategoryId)
            every { seasonWeeks.breakWeeks() } returns emptyMap()
            calcFixtures(seasonId, seasonFixtureDao, seasonTeamDao, seasonCompetitionDao, seasonTeamCategoryDao, teamCategoryDao, seasonCompRoundViewDao, seasonWeeks)

            then("it should delete existing fixtures for the category") {
                verifySuspend {
                    seasonFixtureDao
                        .deleteBySeasonTeamCategory(seasonId, teamCategoryId, competitionId)
                }
            }

            then("it should insert new fixtures") {
                verifySuspend {
                    for (a in 1..4) {
                        for (b in (a + 1)..4) {
                            seasonFixtureDao.insert(hasTeam(a.toShort(), b.toShort()))
                        }
                    }
                }
                verifyNoMoreCalls(seasonFixtureDao)
            }
        }

        When("calcFixtures is called with breaks") {
            setSeasonCompTeams(seasonTeamCategoryDao, seasonTeamDao, competitionId, teamCategoryId)
            every { seasonWeeks.breakWeeks() } returns mapOf(Pair(DayDate("08/01/2025").value(), "MissMe"))
            calcFixtures(seasonId, seasonFixtureDao, seasonTeamDao, seasonCompetitionDao, seasonTeamCategoryDao, teamCategoryDao, seasonCompRoundViewDao, seasonWeeks)

            then("it should delete existing fixtures for the category") {
                verifySuspend {
                    seasonFixtureDao
                        .deleteBySeasonTeamCategory(seasonId, teamCategoryId, competitionId)
                }
            }

            then("it should insert new fixtures") {
                verifySuspend {
                    seasonFixtureDao.insert(hasBreak(teamCategoryId, "08/01/2025", "MissMe"))
                    for (a in 1..4) {
                        for (b in (a + 1)..4) {
                            seasonFixtureDao.insert(hasTeam(a.toShort(), b.toShort()))
                        }
                    }
                }
                verifyNoMoreCalls(seasonFixtureDao)
            }
        }

        When("a tean category has no teams in the competition and calcFixtures is called") {
            setSeasonCompTeams(seasonTeamCategoryDao, seasonTeamDao, competitionId, teamCategoryId, secondTeamCategoryId)
            every { seasonWeeks.breakWeeks() } returns emptyMap()
            calcFixtures(seasonId, seasonFixtureDao, seasonTeamDao, seasonCompetitionDao, seasonTeamCategoryDao, teamCategoryDao, seasonCompRoundViewDao, seasonWeeks)

            then("it should delete existing fixtures for the categories") {
                verifySuspend {
                    seasonFixtureDao
                        .deleteBySeasonTeamCategory(seasonId, teamCategoryId, competitionId)
                    seasonFixtureDao
                        .deleteBySeasonTeamCategory(seasonId, secondTeamCategoryId, competitionId)
                }
            }

            then("it should insert new fixtures") {
                verifySuspend {
                    for (a in 1..4) {
                        for (b in (a + 1)..4) {
                            seasonFixtureDao.insert(hasTeam(a.toShort(), b.toShort()))
                        }
                    }
                }
                verifyNoMoreCalls(seasonFixtureDao)
            }
        }
    }

    given("teamName tests") {
        val fixture = SeasonFixtureView(1, seasonId, competitionId, teamCategoryId, "U10", DayDate("01/01/2025").value(), "", "Assoc A", 1, "Assoc B", 2)

        When("teamCountMap is null") {
            then("it should return empty string") {
                teamName(fixture, true, null) shouldBe ""
                teamName(fixture, false, null) shouldBe ""
            }
        }

        When("team count is 0") {
            val teamCountMap: TeamCountMap = mapOf(Triple(teamCategoryId, "Assoc A", competitionId) to 0.toShort())
            then("it should return empty string") {
                teamName(fixture, true, teamCountMap) shouldBe ""
            }
        }

        When("team count is 1") {
            val teamCountMap: TeamCountMap = mapOf(Triple(teamCategoryId, "Assoc A", competitionId) to 1.toShort())
            then("it should return the association name") {
                teamName(fixture, true, teamCountMap) shouldBe "Assoc A"
            }
        }

        When("team count is more than 1") {
            val teamCountMap: TeamCountMap = mapOf(
                Triple(teamCategoryId, "Assoc A", competitionId) to 2.toShort(),
                Triple(teamCategoryId, "Assoc B", competitionId) to 2.toShort()
            )
            then("it should return the association name with postfix") {
                teamName(fixture, true, teamCountMap) shouldBe "Assoc A A"
                teamName(fixture, false, teamCountMap) shouldBe "Assoc B B"
            }
        }

        When("calling teamName with association and number") {
            then("it should return correct postfix") {
                teamName("Assoc", 0) shouldBe "Assoc"
                teamName("Assoc", 1) shouldBe "Assoc A"
                teamName("Assoc", 2) shouldBe "Assoc B"
            }
        }
    }

    given("getFixtures tests") {
        val teamCategories = listOf(TeamCategory(teamCategoryId, "U10", 0.toShort()), TeamCategory(secondTeamCategoryId, "U12", 1.toShort()))
        val teamCounts: TeamCountMap = mapOf(
            Triple(teamCategoryId, "Assoc A", competitionId) to 1.toShort(),
            Triple(teamCategoryId, "Assoc B", competitionId) to 1.toShort(),
            Triple(secondTeamCategoryId, "Assoc A", competitionId) to 1.toShort()
        )
        val date1 = DayDate("01/01/2025").value()
        val date2 = DayDate("08/01/2025").value()
        val date3 = DayDate("15/01/2025").value()
        val date4 = DayDate("22/01/2025").value()
        val fixtures = listOf(
            SeasonFixtureView(1, seasonId, competitionId, teamCategoryId, "U10", date1, "Msg 1", "Assoc A", 1, "Assoc B", 1),
            SeasonFixtureView(2, seasonId, competitionId, secondTeamCategoryId, "U12", date1, "Msg 2", "Assoc A", 1, "", 0),
            SeasonFixtureView(3, seasonId, competitionId, teamCategoryId, "U10", date2, "Msg 3", "Assoc B", 1, "Assoc A", 1),
            SeasonFixtureView(4, seasonId, competitionId, teamCategoryId, "U10", date3, "Break 10", "", 0, "", 0),
            SeasonFixtureView(5, seasonId, competitionId, teamCategoryId, "U12", date4, "Break 12", "", 0, "", 0)
        )

        When("no filters are applied") {
            val results = mutableListOf<List<String>>()
            getFixtures(fixtures, competitionId, "", "", teamCategories, teamCounts) { date, cat, msg, home, away ->
                results.add(listOf(date, cat, msg, home, away))
            }
            then("it should return all fixtures in order") {
                results.size shouldBe 5
                results[0] shouldBe listOf("01/01/2025", "U10", "Msg 1", "Assoc A", "Assoc B")
                results[1] shouldBe listOf("02/01/2025", "U12", "Msg 2", "Assoc A", "") // Adjusted date (+1 day for U12)
                results[2] shouldBe listOf("08/01/2025", "U10", "Msg 3", "Assoc B", "Assoc A")
                results[3] shouldBe listOf("15/01/2025", "U10", "Break 10", "", "")
                results[4] shouldBe listOf("23/01/2025", "U12", "Break 12", "", "")
            }
        }

        When("filtering by association") {
            val results = mutableListOf<List<String>>()
            getFixtures(fixtures, competitionId, "Assoc A", "", teamCategories, teamCounts) { date, cat, msg, home, away ->
                results.add(listOf(date, cat, msg, home, away))
            }
            then("it should only return fixtures containing that association") {
                results.size shouldBe 5 // All fixtures in this case involve Assoc A
            }

            val resultsB = mutableListOf<List<String>>()
            getFixtures(fixtures, competitionId, "Assoc B", "", teamCategories, teamCounts) { date, cat, msg, home, away ->
                resultsB.add(listOf(date, cat, msg, home, away))
            }
            then("it should only return Assoc B fixtures") {
                resultsB.size shouldBe 4
                resultsB[0][2] shouldBe "Msg 1"
                resultsB[1][2] shouldBe "Msg 3"
                resultsB[2][2] shouldBe "Break 10"
                resultsB[3][2] shouldBe "Break 12"
            }
        }

        When("filtering by team category") {
            val results = mutableListOf<List<String>>()
            getFixtures(fixtures, competitionId, "", "U12", teamCategories, teamCounts) { date, cat, msg, home, away ->
                results.add(listOf(date, cat, msg, home, away))
            }
            then("it should only return fixtures for that category") {
                results.size shouldBe 2
                results[0][1] shouldBe "U12"
                results[1][2] shouldBe "Break 12"
            }
        }
    }
})

private fun setSeasonCompTeams(
    seasonTeamCategoryDao: SeasonTeamCategoryDao,
    seasonTeamDao: SeasonTeamDao,
    competitionId: CompetitionId,
    vararg teamCategoryIds: TeamCategoryId
) {
    val activeTeamCategories = teamCategoryIds.map {
        seasonTeamCategoryOf(competitionId, it, if (it == teamCategoryId) 1 else 0)
    }
    val seasonTeams = teamCategoryIds.map { seasonTeamOf(competitionId, it) }
    everySuspend {
        seasonTeamCategoryDao.getActiveTeamCategories(seasonId, competitionId)
    } returns activeTeamCategories
    everySuspend {
        seasonTeamCategoryDao.getBySeasonId(seasonId)
    } returns activeTeamCategories
    everySuspend {
        seasonTeamDao.getBySeason(seasonId)
    } returns seasonTeams

    for (teamCategoryId in teamCategoryIds) {
        everySuspend { seasonTeamDao.getTeams(seasonId, competitionId, teamCategoryId)
        } returns listOf(seasonTeamOf(competitionId, teamCategoryId) )
    }
}

private fun MokkeryMatcherScope.hasBreak(teamCategoryId: TeamCategoryId, date: String, message: String): SeasonFixture = matches(
    toString = { "hasBreak($date - $message" },
    predicate = { it.teamCategoryId == teamCategoryId && it.date == DayDate(date).value() && it.message == message }
)

private fun MokkeryMatcherScope.hasTeam(first : TeamNumber, second : TeamNumber): SeasonFixture = matches(
    toString = { "hasTeam($associationId - $first / $second)" },
    predicate = { it.homeAssociationId == associationId && it.awayAssociationId == associationId &&
            ((it.homeTeamNumber == first && it.awayTeamNumber == second) || (it.awayTeamNumber == first && it.homeTeamNumber == second)) }
)

private fun seasonTeamOf(competitionId: CompetitionId, teamCategoryId: TeamCategoryId): SeasonTeam =
    SeasonTeam(seasonId, competitionId, associationId, teamCategoryId, 4)

private fun seasonTeamCategoryOf(competitionId: CompetitionId, teamCategoryId: TeamCategoryId, games: Short = 1): SeasonTeamCategory =
    SeasonTeamCategory(seasonId, competitionId, teamCategoryId, games, false)

private fun seasonCompetitionOf(competitionId: CompetitionId): SeasonCompetition =
    SeasonCompetition(seasonId, competitionId, DayDate("01/01/2025").value(), DayDate("31/01/2025").value()
)
