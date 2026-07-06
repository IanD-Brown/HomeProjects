package io.github.iandbrown.sportplanner.ui

import io.github.iandbrown.sportplanner.database.Season
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class HomeScreenTest : ShouldSpec({
    context("Editors") {
        should("generate correct viewRoute") {
            Editors.SEASONS.viewRoute() shouldBe Route.SeasonList
        }

        should("generate correct addRoute") {
            Editors.SEASONS.addRoute() shouldBe Route.SeasonEdit(null)
        }

        should("generate correct editRoute for an item") {
            val season = Season(1, "2025")
            val expected = Route.SeasonEdit(season)
            Editors.SEASONS.editRoute(season) shouldBe expected
        }

        should("generate correct viewRoute for an item") {
            val season = Season(1, "2025")
            val expected = Route.SeasonBreakList(season)
            Editors.SEASON_BREAK.viewRoute(season) shouldBe expected
        }

        should("only show specific editors on home screen") {
            val visibleEditors = Editors.entries.filter { it.showOnHome }
            visibleEditors.map { it.name } shouldBe listOf(
                "SEASON_LEAGUE_FIXTURES",
                "SEASON_CUP_FIXTURES",
                "SEASONS",
                "COMPETITIONS",
                "TEAM_CATEGORIES",
                "ASSOCIATIONS",
                "FAR_ASSOCIATIONS"
            )
        }
    }
})
