package io.github.iandbrown.home_energy.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 2
private const val majorVersion = 1

@Database(entities = [
        Meter::class,
        MeterTariff::class,
        Setting::class,
        Usage::class,
    ],
    views = [],
    version = version,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getMeterDao(): MeterDao
    abstract fun getMeterTariffDao(): MeterTariffDao
    abstract fun getSettingDao(): SettingDao
    abstract fun getUsageDao(): UsageDao
}

const val dbFileName = "HomeEnergyDb$majorVersion.db"
