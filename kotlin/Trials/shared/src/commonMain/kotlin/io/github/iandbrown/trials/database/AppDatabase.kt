package io.github.iandbrown.trials.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 2
private const val majorVersion = 1

@Database(entities = [
        AdultDetails::class,
        EmailTemplate::class,
        NextState::class,
        Nomination::class,
        Setting::class,
        State::class,
        TeamCategory::class,
        Trial::class
    ],
    views = [],
    version = version,
    autoMigrations = [
        AutoMigration(1, 2)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAdultDetailsDao(): AdultDetailsDao
    abstract fun getEmailTemplateDao(): EmailTemplateDao
    abstract fun getNextStateDao(): NextStateDao
    abstract fun getNominationDao(): NominationDao
    abstract fun getSettingDao(): SettingDao
    abstract fun getStateDao(): StateDao
    abstract fun getTeamCategoryDao(): TeamCategoryDao
    abstract fun getTrialDao(): TrialDao
}

const val dbFileName = "TrialsDb$majorVersion.db"
