package io.github.iandbrown.sportplanner.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 8
private const val majorVersion = 3

@Database(entities = [
    Association::class,
    Competition::class,
    Season::class,
    SeasonBreak::class,
    SeasonCompetition::class,
    SeasonCompetitionRound::class,
    SeasonCupFixture::class,
    SeasonFixture::class,
    SeasonTeam::class,
    SeasonTeamCategory::class,
    TeamCategory::class],
    views = [
        SeasonCompRoundView::class,
        SeasonCompView::class,
        SeasonCupFixtureView::class,
        SeasonFixtureView::class,
        SeasonLeagueTeamView::class],
    version = version,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAssociationDao() : AssociationDao
    abstract fun getCompetitionDao() : CompetitionDao
    abstract fun getSeasonBreakDao() : SeasonBreakDao
    abstract fun getSeasonCompViewDao() : SeasonCompViewDao
    abstract fun getSeasonCompRoundViewDao() : SeasonCompRoundViewDao
    abstract fun getSeasonCompetitionDao() : SeasonCompetitionDao
    abstract fun getSeasonCompetitionRoundDao() : SeasonCompetitionRoundDao
    abstract fun getSeasonCupFixtureDao() : SeasonCupFixtureDao
    abstract fun getSeasonCupFixtureViewDao() : SeasonCupFixtureViewDao
    abstract fun getSeasonDao() : SeasonDao
    abstract fun getSeasonFixtureDao() : SeasonFixtureDao
    abstract fun getSeasonFixtureViewDao() : SeasonFixtureViewDao
    abstract fun getSeasonLeagueTeamViewDao() : SeasonLeagueTeamViewDao
    abstract fun getSeasonTeamCategoryDao() : SeasonTeamCategoryDao
    abstract fun getSeasonTeamDao() : SeasonTeamDao
    abstract fun getTeamCategoryDao() : TeamCategoryDao
}

const val dbFileName = "SportPlanningDb$majorVersion.db"
