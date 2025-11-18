package io.github.iandbrown.sportplanner.database

expect class DBFactory {
    fun createDatabase(): AppDatabase
}