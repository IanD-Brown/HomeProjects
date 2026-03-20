package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
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
    suspend fun deleteAll()

    @Query("DELETE FROM $table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : Int?

    @Query("SELECT * FROM $table")
    suspend fun getDefinitions() : List<ImportDefinition>
}
