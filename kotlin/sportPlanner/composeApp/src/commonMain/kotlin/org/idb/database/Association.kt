package org.idb.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Associations",
    indices = [Index(value = ["name"], unique = true)])
data class Association(
    @PrimaryKey(autoGenerate = true)
    val id : Short = 0,
    var name: String)