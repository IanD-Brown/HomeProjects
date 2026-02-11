package io.github.iandbrown.sportplanner.ui

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.github.iandbrown.sportplanner.database.SeasonCupFixture
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureDao
import io.github.iandbrown.sportplanner.database.SeasonCupFixtureView
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class SeasonCompetitionRoundViewTest : ShouldSpec({
    val seasonId = 1.toShort()
    val competitionId = 2.toShort()
    val teamCategoryId = 3.toShort()

    context("roundUpToNextPowerOfTwo") {
        should("return 1 for 0") {
            roundUpToNextPowerOfTwo(0) shouldBe 1
        }

        should("return 1 for negative values") {
            roundUpToNextPowerOfTwo(-1) shouldBe 1
            roundUpToNextPowerOfTwo(-10) shouldBe 1
        }

        should("return same value if it's already a power of two") {
            var input = 1
            while (input < 1 shl 10) {
                roundUpToNextPowerOfTwo(input) shouldBe input
                input *= 2
            }
        }

        should("return next power of two for non-power values") {
            roundUpToNextPowerOfTwo(3) shouldBe 4
            roundUpToNextPowerOfTwo(5) shouldBe 8
            roundUpToNextPowerOfTwo(7) shouldBe 8
            roundUpToNextPowerOfTwo(9) shouldBe 16
            roundUpToNextPowerOfTwo(15) shouldBe 16
            roundUpToNextPowerOfTwo(17) shouldBe 32
            roundUpToNextPowerOfTwo(1000) shouldBe 1024
        }
    }

    context("planFixtures") {
        should("plan 2 games for 4 teams correctly") {
            val teams = listOf(pairOf(1, 1), pairOf(1, 2), pairOf(2, 1), pairOf(2, 2))
            val result = planFixtures(2, teams)
            result.size shouldBe 2
            result[0] shouldBe CupFixtureTeams(1, 1, 1, 2)
            result[1] shouldBe CupFixtureTeams(2, 1, 2, 2)
        }

        should("plan 4 games for 6 teams correctly (2 byes)") {
            val teams = listOf(
                pairOf(1, 1),
                pairOf(1, 2),
                pairOf(1, 3), pairOf(1, 4),
                pairOf(2, 5), pairOf(3, 6))
            val result = planFixtures(4, teams)
            result.size shouldBe 4
            result[0] shouldBe CupFixtureTeams(1, 1, 0, 0)
            result[1] shouldBe CupFixtureTeams(1, 2, 0, 0)
            result[2] shouldBe CupFixtureTeams(1, 3, 1, 4)
            result[3] shouldBe CupFixtureTeams(2, 5, 3, 6)
        }
    }

    context("calcCupFixtures") {
        val seasonTeamDao = mock<SeasonTeamDao>()
        val dao = mock<SeasonCupFixtureDao>()
        val teamCategoryDao = mock<TeamCategoryDao>()

        should("calculate round 1 fixtures correctly for 4 teams") {
            val round = 1.toShort()
            val teamCategory = TeamCategory(teamCategoryId, "U10", 0.toShort())
            val teams = listOf(SeasonTeam(seasonId, competitionId, 1, teamCategoryId, 2), SeasonTeam(seasonId, competitionId, 2, teamCategoryId, 2))

            everySuspend { dao.deleteByRound(seasonId, competitionId, round) } returns Unit
            everySuspend { teamCategoryDao.getAsList() } returns listOf(teamCategory)
            everySuspend { seasonTeamDao.getTeams(seasonId, competitionId, teamCategoryId) } returns teams
            everySuspend { dao.insert(any()) } returns 0L

            calcCupFixtures(seasonId, competitionId, round, seasonTeamDao, dao, teamCategoryDao)

            verifySuspend {
                dao.deleteByRound(seasonId, competitionId, round)
                dao.insert(any()) // 2 games
                dao.insert(any())
            }
        }

        should("calculate round 2 fixtures correctly based on round 1 results") {
            val round = 2.toShort()
            val teamCategory = TeamCategory(teamCategoryId, "U10", 0.toShort())
            val round1Fixtures = listOf(
                SeasonCupFixture(1, seasonId, competitionId, 1.toShort(), teamCategoryId, 1, 0, 1, 1, 0L, 0L, 1.toShort()), // Home win
                SeasonCupFixture(2, seasonId, competitionId, 1.toShort(), teamCategoryId, 2, 0, 2, 1, 0L, 0L, 2.toShort())  // Away win
            )

            everySuspend { dao.deleteByRound(seasonId, competitionId, round) } returns Unit
            everySuspend { teamCategoryDao.getAsList() } returns listOf(teamCategory)
            everySuspend { dao.get(seasonId, competitionId, teamCategoryId, 1.toShort()) } returns round1Fixtures
            everySuspend { dao.insert(any()) } returns 0L

            calcCupFixtures(seasonId, competitionId, round, seasonTeamDao, dao, teamCategoryDao)

            verifySuspend {
                dao.deleteByRound(seasonId, competitionId, round)
                dao.insert(any()) // 1 game: Winner of F1 vs Winner of F2
            }
        }
    }

    context("getFixtures") {
        val round = 1.toShort()
        val fixtures = listOf(
            SeasonCupFixtureView(1, seasonId, competitionId, round, teamCategoryId, "U10", "Assoc A", 0, "Assoc B", 0, 0, 0, 0),
            SeasonCupFixtureView(2, seasonId, competitionId, round, (teamCategoryId + 1).toShort(), "U12", "Assoc C", 0, "Assoc D", 0, 0, 0, 0)
        )

        should("return all fixtures when no filter is applied") {
            val results = mutableListOf<String>()
            getFixtures(fixtures, round, "", emptyMap()) { cat, _, _, _, _, _ ->
                results.add(cat)
            }
            results.size shouldBe 2
            results[0] shouldBe "U10"
            results[1] shouldBe "U12"
        }

        should("return filtered fixtures when a category is specified") {
            val results = mutableListOf<String>()
            getFixtures(fixtures, round, "U12", emptyMap()) { cat, _, _, _, _, _ ->
                results.add(cat)
            }
            results.size shouldBe 1
            results[0] shouldBe "U12"
        }
    }

    context("teamDescription") {
        should("return team name for non-pending fixture") {
            teamDescription(emptyMap(), 0L, "Assoc A", 1) shouldBe "Assoc A A"
            teamDescription(emptyMap(), 0L, "Assoc A", 0) shouldBe "Assoc A"
        }

        should("return winner of previous fixture if played") {
            val prevFixtureId = 10L
            val fixturesById = mapOf(
                prevFixtureId to SeasonCupFixtureView(prevFixtureId, seasonId, competitionId, 1, teamCategoryId, "U10", "Assoc A", 0, "Assoc B", 0, 0L, 0L, 1.toShort()) // Home win
            )
            teamDescription(fixturesById, prevFixtureId, "", 0) shouldBe "Assoc A"
        }

        should("return OR string if previous fixture not played") {
            val prevFixtureId = 10L
            val fixturesById = mapOf(
                prevFixtureId to SeasonCupFixtureView(prevFixtureId, seasonId, competitionId, 1, teamCategoryId, "U10", "Assoc A", 0, "Assoc B", 0, 0L, 0L, 0.toShort()) // Unplayed
            )
            teamDescription(fixturesById, prevFixtureId, "", 0) shouldBe "Assoc A OR Assoc B"
        }
    }
})

private fun pairOf(first: Int, second : Int) : Pair<Short, Short> = Pair(first.toShort(), second.toShort())
