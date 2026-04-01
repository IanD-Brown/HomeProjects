package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import io.github.iandbrown.reconciler.di.inject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "ImportDefinitions"

@Serializable
@Entity(tableName = table)
data class ImportDefinition(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = ""
)

@Dao
interface ImportDefinitionDao : BaseReadDao<ImportDefinition>, BaseWriteDao<ImportDefinition> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<ImportDefinition>>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("DELETE FROM $table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : Int?

    @Query("SELECT * FROM $table")
    suspend fun getDefinitions() : List<ImportDefinition>

    @Transaction
    suspend fun save(importId: Int, name: String, importDefinitions: (Int) -> List<AccountImportDefinition>,
                     accountImportDefinitionDao: AccountImportDefinitionDao = inject<AccountImportDefinitionDao>().value) {
        if (importId == 0) {
            insert(ImportDefinition(name = name))
        } else {
            update(ImportDefinition(importId, name))
        }

        for (importDefinition in importDefinitions(getByName("name")!!)) {
            accountImportDefinitionDao.insert(importDefinition)
        }

    }
}
