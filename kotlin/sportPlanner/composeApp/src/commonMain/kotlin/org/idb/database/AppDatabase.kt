package org.idb.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [Association::class, Season::class, TeamCategory::class], version = 6)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAssociationDao() : AssociationDao
    abstract fun getSeasonDao() : SeasonDao
    abstract fun getTeamCategoryDao() : TeamCategoryDao
}

// The Room compiler generates the `actual` implementations.
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "sport_db.db"
