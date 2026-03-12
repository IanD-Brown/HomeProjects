package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "TransactionCategories"

@Serializable
@Entity(
    tableName = table,
    indices = [Index(value = ["name"], unique = true)]
)
data class TransactionCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val filter: Boolean,
    val isSpending: Boolean = true
)

@Dao
interface TransactionCategoryDao : BaseReadDao<TransactionCategory>, BaseWriteDao<TransactionCategory> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<TransactionCategory>>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()
}
