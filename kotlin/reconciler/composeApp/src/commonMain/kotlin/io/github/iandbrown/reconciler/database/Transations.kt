package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "Transactions"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["account", "rowIndex"],
    indices = [Index(value = ["account"]), Index(value = ["category"])],
    foreignKeys = [ForeignKey(
        entity = Account::class,
        parentColumns = ["id"],
        childColumns = ["account"],
        onDelete = ForeignKey.CASCADE),
        ForeignKey(
        entity = TransactionCategory::class,
        parentColumns = ["id"],
        childColumns = ["category"],
        onDelete = ForeignKey.SET_NULL)]
)
data class Transaction(
    val account: Int,
    val rowIndex: Int,
    val date: Int,
    val description: String,
    val amount: Double,
    val category: Int? = null
)

@Dao
interface TransactionDao : BaseReadDao<Transaction>, BaseWriteDao<Transaction> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<Transaction>>

    @Query("DELETE FROM $table WHERE account = :account")
    suspend fun deleteAllByAccount(account: Int)

    @Query("UPDATE $table SET category = :category")
    suspend fun resetAllCategories(category : Int = 0)

    @Query("UPDATE $table SET category = :category WHERE account = :account AND rowIndex = :rowIndex")
    suspend fun setCategory(account: Int, rowIndex: Int, category: Int?)

    @Query("SELECT * FROM $table WHERE category = :category")
    suspend fun getByCategory(category : Int = 0) : List<Transaction>

    @Query("SELECT * FROM $table WHERE category IS NULL")
    suspend fun getByUnknownCategory() : List<Transaction>}
