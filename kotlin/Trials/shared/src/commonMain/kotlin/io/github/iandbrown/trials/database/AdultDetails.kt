package io.github.iandbrown.trials.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "AdultDetails"

@Serializable
enum class AdultType(val displayName: String) {
    NOMINEE("Nominee"),
    PARENT_OR_CARER("Parent/Carer")
}

@Serializable
@Entity(tableName = table)
data class AdultDetails(
    @ColumnInfo(defaultValue = "PARENT_OR_CARER")
    val type: AdultType = AdultType.PARENT_OR_CARER,

    @ColumnInfo(defaultValue = "")
    val emailAddress: String = "",

    @ColumnInfo(defaultValue = "")
    val firstNameOfParentOrCarer: String = "",

    @ColumnInfo(defaultValue = "")
    val surnameOfParentOrCarer: String = "",

    @ColumnInfo(defaultValue = "")
    val parentOrCarerMobileNumber: String = "",

    @ColumnInfo(defaultValue = "")
    val confirmEmail: String = "",

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(defaultValue = "0")
    val id: Int = 0,
)

@Dao
interface AdultDetailsDao : BaseReadDao<AdultDetails>, BaseWriteDao<AdultDetails> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<AdultDetails>

    @Query("SELECT * FROM $table WHERE emailAddress = :emailAddress AND firstNameOfParentOrCarer = :firstName AND surnameOfParentOrCarer = :surname AND type = :type")
    suspend fun getByDetails(emailAddress: String, firstName: String, surname: String, type: AdultType): AdultDetails?

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
