package io.github.iandbrown.sportplanner.ui

import io.github.iandbrown.sportplanner.database.Season
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class HomeScreenTest : ShouldSpec({
    context("Editors") {
        should("generate correct viewRoute") {
            Editors.SEASONS.viewRoute() shouldBe "SEASONS/View"
        }

        should("generate correct addRoute") {
            Editors.SEASONS.addRoute() shouldBe "SEASONS/Add"
        }

        should("generate correct editRoute for an item") {
            val season = Season(1, "2025")
            val expected = "SEASONS/${Json.encodeToString(season)}"
            Editors.SEASONS.editRoute(season) shouldBe expected
        }

        should("generate correct viewRoute for an item") {
            val season = Season(1, "2025")
            val expected = "SEASONS/View&${Json.encodeToString(season)}"
            Editors.SEASONS.viewRoute(season) shouldBe expected
        }
    }
})
