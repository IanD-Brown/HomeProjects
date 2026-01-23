package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

private const val viewName = "SeasonCupFixtureView"

@DatabaseView(viewName = viewName,
    value = "SELECT scf.id AS id, " +
            "scf.seasonId AS seasonId, " +
            "scf.competitionId AS competitionId, " +
            "scf.teamCategoryId AS teamCategoryId, " +
            "tc.name AS teamCategoryName, " +
            "scf.round AS round, " +
            "h.name AS homeAssociation, " +
            "scf.homeTeamNumber AS homeTeamNumber, " +
            "a.name AS awayAssociation, " +
            "scf.awayTeamNumber AS awayTeamNumber, " +
            "scf.homePending AS homePending, " +
            "scf.awayPending AS awayPending, " +
            "scf.result AS result " +
            "FROM SeasonCupFixtures scf " +
            "LEFT JOIN TeamCategories tc ON tc.id = scf.teamCategoryId " +
            "LEFT JOIN Associations h ON h.id = scf.homeAssociationId " +
            "LEFT JOIN Associations a ON a.id = scf.awayAssociationId")
data class SeasonCupFixtureView(
    val id : Long,
    val seasonId: SeasonId,
    val competitionId: CompetitionId,
    val round: Short,
    val teamCategoryId: Short,
    val teamCategoryName: String,
    val homeAssociation: AssociationName,
    val homeTeamNumber: TeamNumber,
    val awayAssociation: AssociationName,
    val awayTeamNumber: TeamNumber,
    val homePending : Long,
    val awayPending : Long,
    val result : Short
)

@Dao
interface SeasonCupFixtureViewDao : BaseSeasonCompReadDao<SeasonCupFixtureView> {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId AND competitionId = :competitionId")
    override suspend fun get(seasonId : SeasonId, competitionId : CompetitionId): List<SeasonCupFixtureView>

    @Query("UPDATE SeasonCupFixtures SET result = :result WHERE id = :id")
    suspend fun setResult(id: Long, result: Short)
}
