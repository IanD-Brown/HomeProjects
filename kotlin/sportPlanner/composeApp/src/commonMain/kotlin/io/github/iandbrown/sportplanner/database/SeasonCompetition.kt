package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
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
    val seasonId : Short,
    val competitionId : Short,
    var startDate: Int,
    var endDate: Int
) {
    fun isValid() : Boolean = startDate in 1..endDate
}

@Dao
interface SeasonCompetitionDao : BaseDao<SeasonCompetition> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonCompetition>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    suspend fun getBySeason(seasonId : Short): List<SeasonCompetition>

    @Query("SELECT * FROM $table "+
            "WHERE seasonId = :seasonId AND startDate > 0 AND endDate > startDate " +
            "AND competitionId IN (SELECT id FROM Competitions WHERE type = 0)")
    suspend fun getActiveLeagueCompetitions(seasonId: Short) : List<SeasonCompetition>
}
