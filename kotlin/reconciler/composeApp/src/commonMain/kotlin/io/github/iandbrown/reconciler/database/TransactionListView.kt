package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val viewName = "TransactionListView"

@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "t.id, " +
            "t.account, " +
            "t.date, " +
            "t.description, " +
            "t.amount, " +
            "t.category, " +
            "tc.name as categoryName, " +
            "a.name As accountName, " +
            "a.accountGroup, " +
            "g.name AS accountGroupName " +
            "FROM Transactions t " +
            "LEFT JOIN TransactionCategories tc ON tc.id = t.category " +
            "LEFT JOIN Accounts a ON a.id = t.account " +
            "LEFT JOIN AccountGroups g ON g.id = a.accountGroup " +
            "ORDER BY t.date")

@Serializable
data class TransactionListView(
    val id: Long,
    val account: Int,
    val date: Int,
    val description: String,
    val amount: Double,
    val category: Int?,
    val categoryName: String,
    val accountName: String,
    val accountGroup: Int,
    val accountGroupName: String
)

@Dao
interface TransactionListViewDao : BaseReadDao<TransactionListView> {
    @Query("SELECT * FROM $viewName")
    override suspend fun get(): List<TransactionListView>

    @Query("SELECT * FROM $viewName")
    suspend fun getAll() : List<TransactionListView>

    @Query("DELETE FROM Transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
