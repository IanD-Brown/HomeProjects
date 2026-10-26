package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "EmailTemplates"

@Serializable
@Entity(tableName = table)
data class EmailTemplate(
    @PrimaryKey val name: String,
    val bodyTemplate: String
)

@Dao
interface EmailTemplateDao : BaseReadDao<EmailTemplate>, BaseWriteDao<EmailTemplate> {
    @Query("SELECT * FROM $table ORDER BY name")
    override suspend fun get(): List<EmailTemplate>

    @Query("SELECT * FROM $table WHERE name = :name")
    suspend fun getByName(name: String): EmailTemplate?

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
