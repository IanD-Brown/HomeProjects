package org.idb.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "Seasons", indices = [Index(value = ["name"], unique = true)])
data class Season(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    var name: String,
    var startDate: Long,
    var endDate: Long
)