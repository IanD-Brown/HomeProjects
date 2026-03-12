package io.github.iandbrown.reconciler.database

import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 1
private const val majorVersion = 1

@Database(entities = [
    ImportDefinition::class,
    Rule::class,
    Transaction::class,
    TransactionCategory::class],
    version = version,
    autoMigrations = [
    ])
abstract class AppDatabase: RoomDatabase() {
    abstract fun getImportDefinitionDao() : ImportDefinitionDao
    abstract fun getRuleDao() : RuleDao
    abstract fun getTransactionDao() : TransactionDao
    abstract fun getTransactionCategoryDao() : TransactionCategoryDao
}

const val dbFileName = "ReconcilerDb$majorVersion.db"
