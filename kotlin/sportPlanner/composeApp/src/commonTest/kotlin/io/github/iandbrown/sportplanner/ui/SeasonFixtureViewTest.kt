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
