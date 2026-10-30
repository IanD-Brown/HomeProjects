package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "TeamCategories"

@Serializable
enum class YearGroup(val displayName: String) {
    FIVE("Five"),
    SIX("Six"),
    SEVEN("Seven"),
    EIGHT("Eight"),
    NINE("Nine");

    companion object {
        fun fromString(value: String): YearGroup =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: FIVE
    }
}

@Serializable
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female");

    companion object {
        fun fromString(value: String): Gender =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: MALE
    }
}

@Serializable
@Entity(tableName = table)
data class TeamCategory(
    @PrimaryKey val name: String,
    val yearGroup: YearGroup = YearGroup.FIVE,
    val gender: Gender = Gender.MALE
)

@Dao
interface TeamCategoryDao : BaseReadDao<TeamCategory>, BaseWriteDao<TeamCategory> {
    @Query("SELECT * FROM $table ORDER BY name")
    override suspend fun get(): List<TeamCategory>

    @Query("SELECT * FROM $table WHERE name = :name")
    suspend fun getByName(name: String): TeamCategory?

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
