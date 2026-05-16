package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "Transactions"

@Serializable
@Entity(
    tableName = table,
    indices = [
        Index(value = ["account"]),
        Index(value = ["category"]),
        Index(value = ["account", "date", "description", "amount"])],
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val account: Int,
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

    @Query("UPDATE $table SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: Int?)

    @Query("SELECT * FROM $table WHERE category = :category")
    suspend fun getByCategory(category : Int = 0) : List<Transaction>

    @Query("SELECT * FROM $table WHERE category IS NULL")
    suspend fun getByUnknownCategory() : List<Transaction>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("SELECT EXISTS (SELECT 1 FROM $table WHERE account = :account AND description = :description AND date = :date AND amount = :amount)")
    suspend fun exists(account: Int, description: String, date:Int, amount: Double): Boolean

    @Query("SELECT * FROM $table WHERE account IN (SELECT id from Accounts WHERE accountGroup = :accountGroup)")
    suspend fun getByAccountGroup(accountGroup: Int) : List<Transaction>
}
