package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable


private const val table = "SeasonCompetitions"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["seasonId", "competitionId"],
    indices = [Index(value = ["seasonId"]), Index(value = ["competitionId"])],
    foreignKeys = [ForeignKey(
        entity = Season::class,
        parentColumns = ["id"],
        childColumns = ["seasonId"],
        onDelete = ForeignKey.CASCADE),
    ForeignKey(
        entity = Competition::class,
        parentColumns = ["id"],
        childColumns = ["competitionId"],
        onDelete = ForeignKey.CASCADE)])
data class SeasonCompetition(
    val seasonId : SeasonId,
    val competitionId : CompetitionId,
    var startDate: Int,
    var endDate: Int
) {
    fun isValid() : Boolean = startDate in 1..endDate
}

@Dao
interface SeasonCompetitionDao : BaseSeasonCompReadDao<SeasonCompetition>, BaseWriteDao<SeasonCompetition> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId")
    override fun get(seasonId : SeasonId, competitionId : CompetitionId): Flow<List<SeasonCompetition>>

    @Query("SELECT * FROM $table "+
            "WHERE seasonId = :seasonId AND startDate > 0 AND endDate > startDate " +
            "AND competitionId IN (SELECT id FROM Competitions WHERE type = 0)")
    suspend fun getActiveLeagueCompetitions(seasonId: SeasonId) : List<SeasonCompetition>
}
