package io.github.iandbrown.sportplanner.ui

import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class SeasonBreakViewTest : ShouldSpec({
    context("buildDateRange") {
        should("return correct range for multiple competitions") {
            val competitions = listOf(
                SeasonCompView(1, "S1", 1, "C1", 1, 100, 200),
                SeasonCompView(1, "S1", 2, "C2", 1, 150, 250),
                SeasonCompView(1, "S1", 3, "C3", 1, 50, 180)
            )
            val range = buildDateRange(competitions)
            range.first shouldBe 50
            range.last shouldBe 250
        }

        should("handle competitions with 0 dates") {
            val competitions = listOf(
                SeasonCompView(1, "S1", 1, "C1", 1, 0, 0),
                SeasonCompView(1, "S1", 2, "C2", 1, 100, 200)
            )
            val range = buildDateRange(competitions)
            range.first shouldBe 100
            range.last shouldBe 200
        }

        should("return empty range for no competitions") {
            val range = buildDateRange(emptyList())
            range.first shouldBe 0
            range.last shouldBe 0
        }
    }
})
