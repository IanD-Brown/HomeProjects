package io.github.iandbrown.trials.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

private const val version = 4
private const val majorVersion = 1

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP VIEW IF EXISTS `TrialsView`")
        connection.execSQL("DROP VIEW IF EXISTS `TrialDetailsView`")
        connection.execSQL("CREATE VIEW IF NOT EXISTS `TrialDetailsView` AS SELECT n.firstNameOfChild, n.surnameOfChild, n.gender, n.whichYearGroupIsYourChildInNow, n.primaryPosition, n.secondaryPosition, n.foot, t.date, t.confirmed, t.attended, t.outcome, t.id FROM Nominations n, Trials t WHERE  t.nominee = n.id ORDER BY surnameOfChild, firstNameOfChild")
        connection.execSQL("CREATE VIEW IF NOT EXISTS `TrialsView` AS SELECT DISTINCT tc.name, tc.yearGroup, tc.gender, t.date FROM Nominations n, TeamCategories tc, Trials t WHERE UPPER(n.whichYearGroupIsYourChildInNow) = tc.yearGroup AND UPPER(n.gender) = tc.gender AND t.nominee = n.id ORDER BY name, date ASC")
    }
}

@Database(entities = [
        AdultDetails::class,
        EmailTemplate::class,
        NextState::class,
        Nomination::class,
        NominationAdultDetails::class,
        Setting::class,
        State::class,
        TeamCategory::class,
        Trial::class
    ],
    views = [
        TrialDetailsView::class,
        TrialsView::class,
    ],
    version = version,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getAdultDetailsDao(): AdultDetailsDao
    abstract fun getEmailTemplateDao(): EmailTemplateDao
    abstract fun getNextStateDao(): NextStateDao
    abstract fun getNominationAdultDetailsDao(): NominationAdultDetailsDao
    abstract fun getNominationDao(): NominationDao
    abstract fun getSettingDao(): SettingDao
    abstract fun getStateDao(): StateDao
    abstract fun getStateWithNextStatesDao(): StateWithNextStatesDao
    abstract fun getTeamCategoryDao(): TeamCategoryDao
    abstract fun getTrialDao(): TrialDao
    abstract fun getTrialDetailsViewDao(): TrialDetailsViewDao
    abstract fun getTrialsViewDao(): TrialsViewDao
}

const val dbFileName = "TrialsDb$majorVersion.db"
