package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import kotlinx.serialization.Serializable

private const val table = "NominationAdultDetails"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["nominationId", "adultDetailsId"],
    foreignKeys = [
        ForeignKey(
            entity = Nomination::class,
            parentColumns = ["id"],
            childColumns = ["nominationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AdultDetails::class,
            parentColumns = ["id"],
            childColumns = ["adultDetailsId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["nominationId"]),
        Index(value = ["adultDetailsId"])
    ]
)
data class NominationAdultDetails(
    val nominationId: Int,
    val adultDetailsId: Int
)

data class NominationWithAdultDetails(
    @Embedded val nomination: Nomination,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NominationAdultDetails::class,
            parentColumn = "nominationId",
            entityColumn = "adultDetailsId"
        )
    )
    val adultDetailsList: List<AdultDetails>
)

@Dao
interface NominationAdultDetailsDao : BaseReadDao<NominationAdultDetails>, BaseWriteDao<NominationAdultDetails> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<NominationAdultDetails>

    @Query("SELECT * FROM $table WHERE nominationId = :nominationId")
    suspend fun getForNomination(nominationId: Int): List<NominationAdultDetails>

    @Query("SELECT * FROM $table WHERE adultDetailsId = :adultDetailsId")
    suspend fun getForAdultDetails(adultDetailsId: Int): List<NominationAdultDetails>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
