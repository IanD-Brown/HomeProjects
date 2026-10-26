package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.serialization.Serializable

private const val statesTable = "States"

@Serializable
enum class StateType(val value: Int, val displayName: String) {
    START(0, "Start"),
    INTERMEDIATE(1, "Intermediate"),
    END(2, "End");

    companion object {
        fun fromInt(value: Int): StateType = entries.find { it.value == value } ?: INTERMEDIATE
    }
}

@Serializable
@Entity(
    tableName = statesTable,
    foreignKeys = [
        ForeignKey(
            entity = EmailTemplate::class,
            parentColumns = ["name"],
            childColumns = ["entryEmail"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["entryEmail"])
    ]
)
data class State(
    @PrimaryKey val name: String,
    val entryEmail: String? = null,
    val type: Int
)

data class StateWithNextStates(
    @Embedded val state: State,
    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = NextState::class,
            parentColumn = "stateName",
            entityColumn = "nextStateName"
        )
    )
    val nextStates: List<State>
)

@Dao
interface StateDao : BaseReadDao<State>, BaseWriteDao<State> {
    @Query("SELECT * FROM $statesTable ORDER BY name")
    override suspend fun get(): List<State>

    @Query("SELECT * FROM $statesTable WHERE name = :name")
    suspend fun getByName(name: String): State?

    @Transaction
    @Query("SELECT * FROM $statesTable WHERE name = :name")
    suspend fun getStateWithNextStates(name: String): StateWithNextStates?

    @Query("DELETE FROM $statesTable")
    override suspend fun deleteAll()
}
