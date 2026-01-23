package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query

private const val table = "SeasonCupFixtures"

@Entity(tableName = table,
    indices = [Index(value = ["seasonId", "competitionId", "round"])],
    foreignKeys = [ForeignKey(
        entity = SeasonCompetitionRound::class,
        parentColumns = ["seasonId", "competitionId", "round"],
        childColumns = ["seasonId", "competitionId", "round"],
        onDelete = ForeignKey.CASCADE)],
)
data class SeasonCupFixture(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val seasonId : SeasonId,
    val competitionId : CompetitionId,
    val round : Short,
    val teamCategoryId : TeamCategoryId,
    val homeAssociationId : AssociationId,
    val homeTeamNumber : TeamNumber,
    val awayAssociationId : AssociationId,
    val awayTeamNumber : TeamNumber,
    val homePending: Long = 0,
    val awayPending : Long = 0,
    val result : Short = 0,
)

@Dao
interface SeasonCupFixtureDao : BaseSeasonDao<SeasonCupFixture> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    override suspend fun get(seasonId: SeasonId): List<SeasonCupFixture>

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId AND teamCategoryId = :teamCategoryId AND round = :round")
    suspend fun get(seasonId: SeasonId, competitionId: CompetitionId, teamCategoryId: TeamCategoryId, round: Short): List<SeasonCupFixture>

    @Query("DELETE FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId AND round = :round")
    suspend fun deleteByRound(seasonId : SeasonId, competitionId: CompetitionId, round: Short)
}
