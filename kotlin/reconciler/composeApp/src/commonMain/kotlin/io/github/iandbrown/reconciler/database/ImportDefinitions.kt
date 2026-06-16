package io.github.iandbrown.reconciler.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import io.github.iandbrown.reconciler.di.inject
import kotlinx.serialization.Serializable

private const val table = "ImportDefinitions"

@Serializable
@Entity(tableName = table)
data class ImportDefinition(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    @ColumnInfo(defaultValue = "0")
    val type: Int = 0
)

@Dao
interface ImportDefinitionDao : BaseReadDao<ImportDefinition>, BaseWriteDao<ImportDefinition> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<ImportDefinition>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("DELETE FROM $table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : Int?

    @Transaction
    suspend fun save(importId: Int, name: String, type: Int, accountImportDefinitions: (Int) -> List<AccountImportDefinition>,
                     accountImportDefinitionDao: AccountImportDefinitionDao = inject<AccountImportDefinitionDao>().value) {
        if (importId == 0) {
            insert(ImportDefinition(name = name, type = type))
        } else {
            update(ImportDefinition(importId, name, type))
        }

        val importDefinitionId = getByName(name)!!
        for (accountImportDefinition in accountImportDefinitions(importDefinitionId)) {
            accountImportDefinitionDao.insert(accountImportDefinition)
        }
    }
}
