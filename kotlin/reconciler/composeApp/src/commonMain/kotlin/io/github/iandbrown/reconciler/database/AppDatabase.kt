package io.github.iandbrown.reconciler.database

import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 1
private const val majorVersion = 1

@Database(entities = [
    Rule::class,
    Transaction::class],
    version = version,
    autoMigrations = [
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getRuleDao() : RuleDao
    abstract fun getTransactionDao() : TransactionDao
}

const val dbFileName = "ReconcilerDb$majorVersion.db"
