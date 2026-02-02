package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "SeasonTeams"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["seasonId", "teamCategoryId", "associationId", "competitionId"],
    indices = [Index(value = ["seasonId"]), Index(value = ["teamCategoryId"]), Index(value = ["associationId"]), Index(value = ["competitionId"])],
    foreignKeys = [
        ForeignKey(
            entity = Season::class,
            parentColumns = ["id"],
            childColumns = ["seasonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamCategory::class,
            parentColumns = ["id"],
            childColumns = ["teamCategoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Association::class,
            parentColumns = ["id"],
            childColumns = ["associationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Competition::class,
            parentColumns = ["id"],
            childColumns = ["competitionId"],
            onDelete = ForeignKey.CASCADE
        )]
)
data class SeasonTeam(
    val seasonId: SeasonId,
    val competitionId: CompetitionId,
    val associationId: AssociationId,
    val teamCategoryId: TeamCategoryId,
    val count: Short
)

// Needs tp be season based rather than season and competition
@Dao
interface SeasonTeamDao : BaseSeasonCompReadDao<SeasonTeam>, BaseWriteDao<SeasonTeam> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId")
    override fun get(seasonId : SeasonId, competitionId : CompetitionId): Flow<List<SeasonTeam>>

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    suspend fun getBySeason(seasonId : SeasonId): List<SeasonTeam>

    @Query("SELECT * FROM $table WHERE seasonId=:seasonId AND competitionId = :competitionId AND teamCategoryId = :teamCategoryId")
    suspend fun getTeams(seasonId : SeasonId, competitionId : CompetitionId, teamCategoryId : TeamCategoryId) : List<SeasonTeam>
}
