package io.github.iandbrown.sportplanner.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 11
private const val majorVersion = 3

@Database(entities = [
    Association::class,
    Competition::class,
    FarAssociation::class,
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
        FarAssociationView::class,
        SeasonCompRoundView::class,
        SeasonCompView::class,
        SeasonCupFixtureView::class,
        SeasonCupSummaryView::class,
        SeasonFixtureView::class,
        SeasonLeagueTeamView::class],
    version = version,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAssociationDao() : AssociationDao
    abstract fun getCompetitionDao() : CompetitionDao
    abstract fun getFarAssociationDao() : FarAssociationDao
    abstract fun getFarAssociationViewDao() : FarAssociationViewDao
    abstract fun getSeasonBreakDao() : SeasonBreakDao
    abstract fun getSeasonCompCupFixtureViewDao() : SeasonCupCompFixtureViewDao
    abstract fun getSeasonCompRoundViewDao() : SeasonCompRoundViewDao
    abstract fun getSeasonCompViewDao() : SeasonCompViewDao
    abstract fun getSeasonCompetitionDao() : SeasonCompetitionDao
    abstract fun getSeasonCompetitionRoundDao() : SeasonCompetitionRoundDao
    abstract fun getSeasonCupFixtureDao() : SeasonCupFixtureDao
    abstract fun getSeasonCupFixtureViewDao() : SeasonCupFixtureViewDao
    abstract fun getSeasonCupSummaryViewDao() : SeasonCupSummaryViewDao
    abstract fun getSeasonDao() : SeasonDao
    abstract fun getSeasonFixtureDao() : SeasonFixtureDao
    abstract fun getSeasonFixtureViewDao() : SeasonFixtureViewDao
    abstract fun getSeasonLeagueTeamCategoryDao() : SeasonLeagueTeamCategoryDao
    abstract fun getSeasonLeagueTeamViewDao() : SeasonLeagueTeamViewDao
    abstract fun getSeasonRoundDao() : SeasonRoundDao
    abstract fun getSeasonTeamCategoryDao() : SeasonTeamCategoryDao
    abstract fun getSeasonTeamDao() : SeasonTeamDao
    abstract fun getTeamCategoryDao() : TeamCategoryDao
}

const val dbFileName = "SportPlanningDb$majorVersion.db"
