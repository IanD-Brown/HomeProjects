package io.github.iandbrown.home_energy.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.startup.Initializer
import kotlinx.coroutines.Dispatchers

internal lateinit var applicationContext: Context

internal class ApplicationContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context = context.also {
        applicationContext = it.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

fun builder(): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder(applicationContext, AppDatabase::class.java, dbFileName)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
