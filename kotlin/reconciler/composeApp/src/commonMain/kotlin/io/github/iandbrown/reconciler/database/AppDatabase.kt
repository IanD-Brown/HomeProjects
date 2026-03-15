package io.github.iandbrown.reconciler.database

import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 1
private const val majorVersion = 1

@Database(
    entities = [
        Account::class,
        AccountImportDefinition::class,
        ImportDefinition::class,
        Rule::class,
        Transaction::class,
        TransactionCategory::class],
    views = [
        ImportDefinitionListView::class],
    version = version,
    autoMigrations = [
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getAccountDao(): AccountDao
    abstract fun getAccountImportDefinitionDao(): AccountImportDefinitionDao
    abstract fun getImportDefinitionDao(): ImportDefinitionDao
    abstract fun getImportDefinitionListViewDao(): ImportDefinitionListViewDao
    abstract fun getRuleDao(): RuleDao
    abstract fun getTransactionCategoryDao(): TransactionCategoryDao
    abstract fun getTransactionDao(): TransactionDao
}

const val dbFileName = "AccountsDb$majorVersion.db"
