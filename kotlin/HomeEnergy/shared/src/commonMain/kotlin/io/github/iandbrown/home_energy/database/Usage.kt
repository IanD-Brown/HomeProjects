package io.github.iandbrown.home_energy.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Usages"

@Serializable
@Entity(tableName = table)
data class Usage(
    @PrimaryKey(autoGenerate = true)
    val id : UsageId = 0)

@Dao
interface UsageDao : BaseReadDao<Usage>, BaseWriteDao<Usage> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Usage>
}
