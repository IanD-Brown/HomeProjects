package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Trials"

@Serializable
@Entity(
    tableName = table,
    foreignKeys = [
        ForeignKey(
            entity = Nomination::class,
            parentColumns = ["id"],
            childColumns = ["nominee"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = State::class,
            parentColumns = ["name"],
            childColumns = ["outcome"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["nominee"]),
        Index(value = ["outcome"])
    ]
)
data class Trial(
    val nominee: Int,
    val date: Int,
    val attended: Boolean = false,
    val outcome: String? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)

@Dao
interface TrialDao : BaseReadDao<Trial>, BaseWriteDao<Trial> {
    @Query("SELECT * FROM $table ORDER BY date DESC")
    override suspend fun get(): List<Trial>

    @Query("SELECT * FROM $table WHERE nominee = :nomineeId ORDER BY date DESC")
    suspend fun getForNominee(nomineeId: Int): List<Trial>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
