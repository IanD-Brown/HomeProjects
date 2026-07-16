package io.github.iandbrown.home_energy.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.iandbrown.home_energy.database.AppDatabase
import io.github.iandbrown.home_energy.database.dbFileName
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, dbFileName)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
    }
    single<AppDatabase> { get<RoomDatabase.Builder<AppDatabase>>().build() }
}
