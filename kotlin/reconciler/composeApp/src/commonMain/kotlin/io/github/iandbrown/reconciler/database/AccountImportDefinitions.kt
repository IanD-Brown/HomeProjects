package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "AccountImportDefinitions"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["accountId", "importDefinitionId"],
    indices = [Index(value = ["accountId"]), Index(value = ["importDefinitionId"])],
    foreignKeys = [ForeignKey(
        entity = Account::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = ImportDefinition::class,
            parentColumns = ["id"],
            childColumns = ["importDefinitionId"],
            onDelete = ForeignKey.CASCADE)]
)
data class AccountImportDefinition(
    val accountId: Int,
    val importDefinitionId: Int,
    val active: Boolean,
    val clear: Boolean = false,
    val sheetName: String = "",
    val dateColumn: String = "",
    val descriptionColumn: String = "",
    val amountInColumn: String = "",
    val amountOutColumn: String = ""
)

@Dao
interface AccountImportDefinitionDao : BaseReadDao<AccountImportDefinition>, BaseWriteDao<AccountImportDefinition> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<AccountImportDefinition>>

    @Query("SELECT * FROM $table")
    suspend fun getDefinitions(): List<AccountImportDefinition>
}
