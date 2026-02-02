package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val viewName = "SeasonFixtureView"

// Used as an input into scheduling fixtures via direct dao usage (not view a ViewModel)
@DatabaseView(viewName = viewName,
    value = "SELECT f.id AS id," +
        "f.seasonId AS seasonId, " +
        "f.competitionId AS competitionId, " +
        "f.teamCategoryId AS teamCategoryId, " +
        "tc.name AS teamCategoryName, " +
        "f.date, " +
        "f.message, " +
        "h.name AS homeAssociation, " +
        "f.homeTeamNumber AS homeTeamNumber, " +
        "a.name AS awayAssociation, " +
        "f.awayTeamNumber AS awayTeamNumber " +
        "FROM SeasonFixtures f " +
        "LEFT JOIN TeamCategories tc ON tc.id = f.teamCategoryId " +
        "LEFT JOIN Associations h ON h.id = f.homeAssociationId " +
        "LEFT JOIN Associations a ON a.id = f.awayAssociationId")

data class SeasonFixtureView(
    val id : Long,
    val seasonId: SeasonId,
    val competitionId: CompetitionId,
    val teamCategoryId: TeamCategoryId,
    val teamCategoryName: String,
    val date: Int,
    val message: String,
    val homeAssociation: AssociationName,
    val homeTeamNumber: TeamNumber,
    val awayAssociation: AssociationName,
    val awayTeamNumber: TeamNumber
)

@Dao
interface SeasonFixtureViewDao : BaseSeasonReadDao<SeasonFixtureView> {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId")
    override fun get(seasonId : SeasonId): Flow<List<SeasonFixtureView>>
}
