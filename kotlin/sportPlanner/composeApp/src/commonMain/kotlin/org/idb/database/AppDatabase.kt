package org.idb.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

private const val version = 6

@Database(entities = [
    Association::class,
    Competition::class,
    Season::class,
    SeasonCompetition::class,
    SeasonTeam::class,
    TeamCategory::class], version = version)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAssociationDao() : AssociationDao
    abstract fun getCompetitionDao() : CompetitionDao
    abstract fun getSeasonDao() : SeasonDao
    abstract fun getSeasonCompetitionDao() : SeasonCompetitionDao
    abstract fun getSeasonTeamDao() : SeasonTeamDao
    abstract fun getTeamCategoryDao() : TeamCategoryDao
}

// The Room compiler generates the `actual` implementations.
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "SportPlanningDb$version.db"
