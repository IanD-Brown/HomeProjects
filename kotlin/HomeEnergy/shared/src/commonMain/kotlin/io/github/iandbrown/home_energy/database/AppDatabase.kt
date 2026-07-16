package io.github.iandbrown.home_energy.database

import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 2
private const val majorVersion = 1

@Database(entities = [
        Meter::class,
        MeterTariff::class,
        RawUsage::class,
        Setting::class,
    ],
    views = [],
    version = version,
    autoMigrations = [
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getMeterDao(): MeterDao
    abstract fun getMeterTariffDao(): MeterTariffDao
    abstract fun getRawUsageDao(): RawUsageDao
    abstract fun getSettingDao(): SettingDao
}

const val dbFileName = "HomeEnergyDb$majorVersion.db"
