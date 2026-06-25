package io.github.iandbrown.home_energy.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.iandbrown.home_energy.database.AppDatabase
import io.github.iandbrown.home_energy.database.dbFileName
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        Room.databaseBuilder<AppDatabase>(getFilePath())
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
    }
    single<AppDatabase> { get<RoomDatabase.Builder<AppDatabase>>().build() }
}

private fun getFilePath(): String {
    val path = getAppDataFolder()
    return File(path, dbFileName).absolutePath
}

internal fun getAppDataFolder(): String? {
    val system = System.getProperty("os.name")
    val path = if (system.contains("Windows", true)) {
        System.getenv("APPDATA")
    } else {
        "${System.getProperty("user.home")}/Library/Application Support/"
    }
    return path
}
