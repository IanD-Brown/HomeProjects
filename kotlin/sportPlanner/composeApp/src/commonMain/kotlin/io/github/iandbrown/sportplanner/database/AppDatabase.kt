package io.github.iandbrown.sportplanner.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

private const val version = 4
private const val majorVersion = 3

@Database(entities = [
    Association::class,
    Competition::class,
    Season::class,
    SeasonBreak::class,
    SeasonCompetition::class,
    SeasonCompetitionRound::class,
    SeasonFixture::class,
    SeasonTeam::class,
    SeasonTeamCategory::class,
    TeamCategory::class],
    views = [SeasonFixtureView::class, SeasonCompRoundView::class],
    version = version,
    autoMigrations = [
        AutoMigration(from = 3, to = 4)
    ])
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAssociationDao() : AssociationDao
    abstract fun getCompetitionDao() : CompetitionDao
    abstract fun getSeasonBreakDao() : SeasonBreakDao
    abstract fun getSeasonCompRoundViewDao() : SeasonCompRoundViewDao
    abstract fun getSeasonCompetitionDao() : SeasonCompetitionDao
    abstract fun getSeasonCompetitionRoundDao() : SeasonCompetitionRoundDao
    abstract fun getSeasonDao() : SeasonDao
    abstract fun getSeasonFixtureDao() : SeasonFixtureDao
    abstract fun getSeasonFixtureViewDao() : SeasonFixtureViewDao
    abstract fun getSeasonTeamCategoryDao() : SeasonTeamCategoryDao
    abstract fun getSeasonTeamDao() : SeasonTeamDao
    abstract fun getTeamCategoryDao() : TeamCategoryDao
}

// The Room compiler generates the `actual` implementations.
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "SportPlanningDb$majorVersion.db"
