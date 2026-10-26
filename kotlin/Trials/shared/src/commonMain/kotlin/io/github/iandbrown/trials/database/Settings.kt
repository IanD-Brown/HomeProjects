package io.github.iandbrown.trials.database

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
    val smtpHost: String,
    val userName: String,
    val password : String)

@Dao
interface SettingDao : BaseReadDao<Setting>, BaseWriteDao<Setting> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Setting>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
