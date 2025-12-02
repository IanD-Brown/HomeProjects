package io.github.iandbrown.sportplanner.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual class DBFactory {
    actual fun createDatabase(): AppDatabase =
        Room.databaseBuilder<AppDatabase>(getFilePath())
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    populateDbOnCreate(connection)
                }
            })
            .build()

    private fun getFilePath(): String {
        val system = System.getProperty("os.name")
        val path = if (system.contains("Windows", true)) {
            System.getenv("APPDATA")
        } else {
            "${System.getProperty("user.home")}/Library/Application Support/"

        }
        return File(path, dbFileName).absolutePath }
}
