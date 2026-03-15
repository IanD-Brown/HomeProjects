package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable


private const val table = "Accounts"

@Serializable
@Entity(
    tableName = table,
    indices = [Index(value = ["name"], unique = true)]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)

@Dao
interface AccountDao : BaseReadDao<Account>, BaseWriteDao<Account> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<Account>>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()

    @Query("SELECT * FROM $table")
    suspend fun getAccounts() : List<Account>

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : Int?
}
