package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val nextStatesTable = "NextStates"

@Serializable
@Entity(
    tableName = nextStatesTable,
    primaryKeys = ["stateName", "nextStateName"],
    foreignKeys = [
        ForeignKey(
            entity = State::class,
            parentColumns = ["name"],
            childColumns = ["stateName"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = State::class,
            parentColumns = ["name"],
            childColumns = ["nextStateName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["stateName"]),
        Index(value = ["nextStateName"])
    ]
)
data class NextState(
    val stateName: String,
    val nextStateName: String
)

@Dao
interface NextStateDao : BaseReadDao<NextState>, BaseWriteDao<NextState> {
    @Query("SELECT * FROM $nextStatesTable")
    override suspend fun get(): List<NextState>

    @Query("SELECT * FROM $nextStatesTable WHERE stateName = :stateName")
    suspend fun getNextStatesFor(stateName: String): List<NextState>

    @Query("DELETE FROM $nextStatesTable")
    override suspend fun deleteAll()
}
