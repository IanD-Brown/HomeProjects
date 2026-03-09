package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import io.github.iandbrown.reconciler.ui.TransactionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "Rules"

@Serializable
@Entity(
    tableName = table,
    indices = [Index(value = ["match"], unique = true)]
)
data class Rule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val match: String,
    val type: Int
)

@Dao
interface RuleDao : BaseReadDao<Rule>, BaseWriteDao<Rule> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<Rule>>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()

    @Query("SELECT * FROM $table WHERE type != :category")
    suspend fun getRules(category: Int = TransactionCategory.UNKNOWN.ordinal) : List<Rule>
}
