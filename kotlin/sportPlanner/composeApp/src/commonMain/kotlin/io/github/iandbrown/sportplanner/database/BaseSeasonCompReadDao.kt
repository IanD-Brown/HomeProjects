package io.github.iandbrown.sportplanner.database

interface BaseSeasonCompReadDao<ENTITY> {
    suspend fun get(seasonId : SeasonId, competitionId : CompetitionId): List<ENTITY>
}
