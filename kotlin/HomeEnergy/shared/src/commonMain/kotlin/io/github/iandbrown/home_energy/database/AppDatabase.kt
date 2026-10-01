package io.github.iandbrown.home_energy.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

private const val version = 4
private const val majorVersion = 1

@RenameColumn(tableName = "RawUsages", fromColumnName = "averageConsumption", toColumnName = "consumption")
internal class Migration3To4 : AutoMigrationSpec

@Database(entities = [
        Meter::class,
        MeterTariff::class,
        RawUsage::class,
        Setting::class,
    ],
    views = [],
    version = version,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(3, 4, spec = Migration3To4::class)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getMeterDao(): MeterDao
    abstract fun getMeterTariffDao(): MeterTariffDao
    abstract fun getRawUsageDao(): RawUsageDao
    abstract fun getSettingDao(): SettingDao
}

const val dbFileName = "HomeEnergyDb$majorVersion.db"
