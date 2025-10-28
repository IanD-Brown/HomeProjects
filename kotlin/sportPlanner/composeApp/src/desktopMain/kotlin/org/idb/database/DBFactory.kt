package org.idb.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual class DBFactory {
    actual fun createDatabase(): AppDatabase =
        Room.databaseBuilder<AppDatabase>(getFilePath())
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(true)
                .build()

    private fun getFilePath(): String =
        File(System.getProperty("java.io.tmpdir"), dbFileName).absolutePath
}