package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Query
import kotlinx.serialization.Serializable


private const val table = "SeasonCompetitions"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["seasonId", "competitionId"],
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
    var startDate: Long,
    var endDate: Long
) {
    fun isValid() : Boolean = startDate > 0 && endDate >= startDate
}

@Dao
interface SeasonCompetitionDao : BaseDao<SeasonCompetition> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonCompetition>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    suspend fun getBySeason(seasonId : Short): List<SeasonCompetition>
}