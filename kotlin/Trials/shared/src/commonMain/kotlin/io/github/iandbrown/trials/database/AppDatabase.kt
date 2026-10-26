package io.github.iandbrown.trials.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 3
private const val majorVersion = 1

@Database(entities = [
        EmailTemplate::class,
        NextState::class,
        Nomination::class,
        Setting::class,
        State::class
    ],
    views = [],
    version = version,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getEmailTemplateDao(): EmailTemplateDao
    abstract fun getNextStateDao(): NextStateDao
    abstract fun getNominationDao(): NominationDao
    abstract fun getSettingDao(): SettingDao
    abstract fun getStateDao(): StateDao
}

const val dbFileName = "TrialsDb$majorVersion.db"
