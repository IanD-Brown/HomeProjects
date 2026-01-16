package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

private const val viewName = "SeasonCompView"

@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "s.id AS seasonId, " +
            "s.name AS seasonName, " +
            "c.id AS competitionId, " +
            "c.name AS competitionName, " +
            "c.type AS competitionType, " +
            "sc.startDate, " +
            "sc.endDate " +
            "FROM seasons s, " +
            "Competitions c  " +
            "LEFT JOIN SeasonCompetitions sc ON sc.seasonId = s.id AND sc.competitionId = c.id " +
            "ORDER BY s.name, c.name")

data class SeasonCompView(
    val seasonId: Short,
    val seasonName: String,
    val competitionId: Short,
    val competitionName: String,
    val competitionType: Short,
    val startDate: Int,
    val endDate: Int
)

@Dao
interface SeasonCompViewDao : ReadonlyDao<SeasonCompView> {
    @Query("SELECT * FROM $viewName")
    override suspend fun getAll(): List<SeasonCompView>
}
