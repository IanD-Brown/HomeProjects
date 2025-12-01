package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "SeasonCompetitionRounds"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["seasonId", "competitionId", "round"],
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
    var description : String,
    var week : Long,
    var optional : Boolean
)


@Dao
interface SeasonCompetitionRoundDao : BaseDao<SeasonCompetitionRound> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonCompetitionRound>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}