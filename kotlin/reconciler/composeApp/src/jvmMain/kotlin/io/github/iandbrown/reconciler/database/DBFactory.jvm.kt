package io.github.iandbrown.reconciler.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

fun builder(): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(getFilePath())
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)

private fun getFilePath(): String {
    val system = System.getProperty("os.name")
    val path = if (system.contains("Windows", true)) {
        System.getenv("APPDATA")
    } else {
        "${System.getProperty("user.home")}/Library/Application Support/"

    }
    return File(path, dbFileName).absolutePath
}
