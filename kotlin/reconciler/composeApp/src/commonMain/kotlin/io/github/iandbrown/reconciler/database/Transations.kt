package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "Transactions"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["sheet", "row"]
)
data class Transaction(
    val sheet: Int,
    val row: Int,
    val date: Int,
    val description: String,
    val amount: Double
)

@Dao
interface TransactionDao : BaseReadDao<Transaction>, BaseWriteDao<Transaction> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<Transaction>>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()
}
