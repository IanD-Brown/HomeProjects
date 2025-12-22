package io.github.iandbrown.sportplanner.ui

import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonTeamViewTest {

    @Test
    fun testCountByTeamCategory() {
        val teamCategories = listOf(
            TeamCategory(1, "U10", 1),
            TeamCategory(2, "U11", 2),
            TeamCategory(3, "U12", 3),
        )

        val seasonTeams = listOf(
            SeasonTeam(1, 1, 1, 1, 2),
            SeasonTeam(1, 1, 2, 1, 2),
            SeasonTeam(1, 1, 1, 2, 1),
            SeasonTeam(1, 1, 2, 2, 0),
            SeasonTeam(1, 1, 1, 3, 0),
        )

        val result = countByTeamCategory(teamCategories, 2, seasonTeams)
        assertEquals(2, result[1]) // All associations have 2 teams for U10 category
        assertEquals(-1, result[2]) // Mismatched counts for U11 category
        assertEquals(-1, result[3]) // Not all associations have teams for U12 category
    }
}
