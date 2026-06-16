package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "AccountGroups"

@Serializable
@Entity(
    tableName = table,
    indices = [Index(value = ["name"], unique = true)]
)
data class AccountGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)

@Dao
interface AccountGroupDao : BaseReadDao<AccountGroup>, BaseWriteDao<AccountGroup> {
    @Query("SELECT * FROM $table ORDER BY name")
    override suspend fun get(): List<AccountGroup>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("SELECT * FROM $table")
    suspend fun getAll() : List<AccountGroup>

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : Int?
}
