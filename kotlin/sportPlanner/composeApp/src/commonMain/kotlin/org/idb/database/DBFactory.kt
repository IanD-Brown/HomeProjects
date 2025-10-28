package org.idb.database

expect class DBFactory {
    fun createDatabase(): AppDatabase
}