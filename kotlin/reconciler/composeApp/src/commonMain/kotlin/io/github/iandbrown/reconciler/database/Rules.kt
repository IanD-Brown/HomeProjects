package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Rules"

@Serializable
@Entity(
    tableName = table,
    indices = [Index(value = ["match"], unique = true),
        Index(value = ["category"]),
        Index(value = ["accountGroup"])],
    foreignKeys = [ForeignKey(
        entity = TransactionCategory::class,
        parentColumns = ["id"],
        childColumns = ["category"],
        onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = AccountGroup::class,
            parentColumns = ["id"],
            childColumns = ["accountGroup"],
            onDelete = ForeignKey.CASCADE)]
)
data class Rule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val match: String,
    val category: Int,
    val accountGroup: Int
)

@Dao
interface RuleDao : BaseReadDao<Rule>, BaseWriteDao<Rule> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Rule>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("SELECT * FROM $table")
    suspend fun getRules() : List<Rule>
}
