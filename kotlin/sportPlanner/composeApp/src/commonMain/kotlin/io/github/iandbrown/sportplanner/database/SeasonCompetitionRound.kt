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
interface SeasonCompetitionRoundDao : BaseSeasonCompReadDao<SeasonCompetitionRound>, BaseWriteDao<SeasonCompetitionRound> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId")
    override suspend fun get(seasonId: SeasonId, competitionId: CompetitionId): List<SeasonCompetitionRound>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("SELECT * FROM $table")
    suspend fun getAll() : List<SeasonCompetitionRound>

    @Query("SELECT * FROM $table scr WHERE seasonId = :seasonId " +
            "AND NOT EXISTS (SELECT * FROM SeasonCupFixtures scf WHERE scf.seasonId = scr.seasonId AND scf.competitionId = scr.competitionId AND scf.round = scr.round AND scf.result <> 0)")
    suspend fun getUnstartedRounds(seasonId: SeasonId) : List<SeasonCompetitionRound>
}

@Dao
interface SeasonRoundDao : BaseSeasonReadDao<SeasonCompetitionRound> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    override suspend fun get(seasonId : SeasonId): List<SeasonCompetitionRound>
}
