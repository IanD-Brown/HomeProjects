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
    val type: Int = -1,
    val name: String = "",
    val creditSheetName: String = "",
    val creditDateColumn: String = "",
    val creditDescriptionColumn: String = "",
    val creditAmountInColumn: String = "",
    val creditAmountOutColumn: String = "",
    val currentSheetName: String = "",
    val currentDateColumn: String = "",
    val currentDescriptionColumn: String = "",
    val currentAmountInColumn: String = "",
    val currentAmountOutColumn: String = ""
)

@Dao
interface ImportDefinitionDao : BaseReadDao<ImportDefinition>, BaseWriteDao<ImportDefinition> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<ImportDefinition>>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()
}
