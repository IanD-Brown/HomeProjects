package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "SeasonTeamCategories"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["seasonId", "competitionId", "teamCategoryId"],
    indices = [Index(value = ["seasonId"]), Index(value = ["competitionId"]), Index(value = ["teamCategoryId"])],
    foreignKeys = [
        ForeignKey(
            entity = Season::class,
            parentColumns = ["id"],
            childColumns = ["seasonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Competition::class,
            parentColumns = ["id"],
            childColumns = ["competitionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamCategory::class,
            parentColumns = ["id"],
            childColumns = ["teamCategoryId"],
            onDelete = ForeignKey.CASCADE
        )]
)
data class SeasonTeamCategory(
    val seasonId: SeasonId,
    val competitionId: CompetitionId,
    val teamCategoryId: TeamCategoryId,
    val games: Short,
    val locked: Boolean
)

@Dao
interface SeasonTeamCategoryDao : BaseSeasonCompReadDao<SeasonTeamCategory>, BaseWriteDao<SeasonTeamCategory> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId")
    override fun get(seasonId : SeasonId, competitionId: CompetitionId): Flow<List<SeasonTeamCategory>>

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    suspend fun getBySeasonId(seasonId : SeasonId): List<SeasonTeamCategory>

    @Query("SELECT * FROM $table "+
            "WHERE seasonId = :seasonId AND competitionId = :competitionId AND locked = 0")
    suspend fun getActiveTeamCategories(seasonId : Short, competitionId : Short) : List<SeasonTeamCategory>
}
