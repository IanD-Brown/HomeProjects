package io.github.iandbrown.reconciler.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val viewName = "ImportDefinitionListView"

@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "def.id AS importDefinitionId, " +
            "def.name AS name, " +
            "ad.accountId as accountId, " +
            "a.name as accountName, " +
            "ad.active, " +
            "ad.clear, " +
            "ad.sheetName, " +
            "ad.dateColumn, " +
            "ad.descriptionColumn, " +
            "ad.amountInColumn, " +
            "ad.amountOutColumn " +
            "FROM ImportDefinitions def " +
            "LEFT JOIN AccountImportDefinitions ad ON ad.importDefinitionId = def.id " +
            "LEFT JOIN Accounts a ON a.id = ad.accountId " +
            "ORDER BY def.name, accountName")

@Serializable
data class ImportDefinitionListView(
    val importDefinitionId: Int = 0,
    val name: String = "",
    val accountId: Int = 0,
    val accountName: String = "",
    val active: Boolean = false,
    val clear: Boolean = false,
    val sheetName: String = "",
    val dateColumn: String = "",
    val descriptionColumn: String = "",
    val amountInColumn: String = "",
    val amountOutColumn: String = ""
)

@Dao
interface ImportDefinitionListViewDao : BaseReadDao<ImportDefinitionListView> {
    @Query("SELECT * FROM $viewName")
    override fun get(): Flow<List<ImportDefinitionListView>>

    @Query("SELECT * from $viewName where importDefinitionId = :id")
    fun getById(id: Int) : Flow<ImportDefinitionListView?>
}
