package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import io.github.iandbrown.reconciler.ui.TransactionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "Transactions"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["sheet", "rowIndex"]
)
data class Transaction(
    val sheet: Int,
    val rowIndex: Int,
    val date: Int,
    val description: String,
    val amount: Double,
    val category: Int = TransactionCategory.UNKNOWN.ordinal
)

@Dao
interface TransactionDao : BaseReadDao<Transaction>, BaseWriteDao<Transaction> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<Transaction>>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()

    @Query("UPDATE $table SET category = :category")
    suspend fun resetAllCategories(category : Int = TransactionCategory.UNKNOWN.ordinal)

    @Query("UPDATE $table SET category = :category WHERE sheet = :sheet AND rowIndex = :rowIndex")
    suspend fun setCategory(sheet: Int, rowIndex: Int, category: Int)

    @Query("SELECT * FROM $table WHERE category = :category")
    suspend fun getByCategory(category : Int = TransactionCategory.UNKNOWN.ordinal) : List<Transaction>
}
