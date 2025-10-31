package org.idb.database

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "SeasonTeams",
    primaryKeys = ["seasonId", "teamCategoryId", "associationId"],
    foreignKeys = [ForeignKey(
    entity = Season::class,
    parentColumns = ["id"],
    childColumns = ["seasonId"],
    onDelete = ForeignKey.CASCADE),
    ForeignKey(
        entity = TeamCategory::class,
        parentColumns = ["id"],
        childColumns = ["teamCategoryId"],
        onDelete = ForeignKey.CASCADE),
    ForeignKey(
        entity = Association::class,
        parentColumns = ["id"],
        childColumns = ["associationId"],
        onDelete = ForeignKey.CASCADE)])
data class SeasonTeam(
    val seasonId : Short,
    val teamCategoryId : Short,
    val associationId : Short,
    var count : Short
)