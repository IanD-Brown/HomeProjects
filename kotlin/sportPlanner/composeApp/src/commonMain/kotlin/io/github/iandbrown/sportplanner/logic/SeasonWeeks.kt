package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.CompetitionId

interface SeasonWeeks {
    fun breakWeeks(): Map<Int, String>

    fun competitionWeeks(id: CompetitionId): List<Int>?

    fun competitions(): List<CompetitionId>
}
