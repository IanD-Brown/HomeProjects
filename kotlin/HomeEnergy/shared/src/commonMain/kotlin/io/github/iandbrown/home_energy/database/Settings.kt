package io.github.iandbrown.home_energy.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Settings"

@Serializable
@Entity(tableName = table)
data class Setting(
    @PrimaryKey val id: Long = 1,
    val apiKey: String,
    val apiPassword : String,
    val targetYear: Short,
    val startMonth: Short,
    val initialBalance: Double,
    @ColumnInfo(defaultValue = "0.0") val directDebitAmount: Double,
    @ColumnInfo(defaultValue = "0") val fromYear: Short)

@Dao
interface SettingDao : BaseReadDao<Setting>, BaseWriteDao<Setting> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Setting>
}
