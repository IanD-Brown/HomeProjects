package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
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
    val seasonId: Short,
    val competitionId: Short,
    val associationId: Short,
    val teamCategoryId: Short,
    val count: Short
)

@Dao
interface SeasonTeamDao : BaseSeasonDao<SeasonTeam> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    override suspend fun get(seasonId : Short): List<SeasonTeam>

    @Query("SELECT * FROM $table WHERE seasonId=:seasonId AND competitionId = :competitionId AND teamCategoryId = :teamCategoryId")
    suspend fun getTeams(seasonId : Short, competitionId : Short, teamCategoryId : Short) : List<SeasonTeam>
}
