package io.github.iandbrown.sportplanner.ui

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class SeasonCompetitionRoundViewTest : ShouldSpec({
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
})

private fun pairOf(first: Int, second : Int) : Pair<Short, Short> = Pair(first.toShort(), second.toShort())
