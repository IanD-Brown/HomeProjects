package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "SeasonCompetitionRounds"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["seasonId", "competitionId", "round"],
    indices = [Index(value = ["seasonId"]), Index(value = ["competitionId"])],
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
        )]
)
data class SeasonCompetitionRound(
    val seasonId : Short,
    val competitionId : Short,
    val round : Short,
    val description : String,
    val week : Int,
    val optional : Boolean
)


@Dao
interface SeasonCompetitionRoundDao : BaseSeasonDao<SeasonCompetitionRound> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    override suspend fun get(seasonId : Short): List<SeasonCompetitionRound>
}
