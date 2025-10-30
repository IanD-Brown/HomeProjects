package org.idb.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "TeamCategories",
    indices = [Index(value = ["name"], unique = true)])
data class TeamCategory (
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    var name: String,
    var matchDay: Int)