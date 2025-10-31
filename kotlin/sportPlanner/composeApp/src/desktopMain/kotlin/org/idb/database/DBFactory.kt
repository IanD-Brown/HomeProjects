package org.idb.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.GregorianCalendar

actual class DBFactory {
    actual fun createDatabase(): AppDatabase =
        Room.databaseBuilder<AppDatabase>(getFilePath())
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    for (name in listOf("DONCASTER", "ROTHERHAM", " BARNSLEY", " LEEDS", " EAST RIDING", " SELBY", " HUDDERSFIELD")) {
                        connection.execSQL("INSERT INTO Associations (name) VALUES ('$name')")
                    }

                    for (name in listOf("u11", "u12", "u13", "u14", "u15")) {
                        connection.execSQL("INSERT INTO TeamCategories (name, matchDay) VALUES ('$name Boys', 5)")
                        connection.execSQL("INSERT INTO TeamCategories (name, matchDay) VALUES ('$name Girls', 6)")
                    }

                    var date = 1
                    var date2 = 2
                    for (name in listOf("20-21", "21-22", "22-23", "24-25", "25-26")) {
                        val ms1 = GregorianCalendar(2019 + date, 9, 1).toInstant().toEpochMilli()
                        val ms2 = GregorianCalendar(2019 + date2, 4, 1).toInstant().toEpochMilli()
                        connection.execSQL("INSERT INTO Seasons (name, startDate, endDate) VALUES ('$name', $ms1, $ms2)")
                        ++date
                        ++date2
                    }
                }
            })
            .build()

    private fun getFilePath(): String =
        File(System.getProperty("java.io.tmpdir"), dbFileName).absolutePath
}