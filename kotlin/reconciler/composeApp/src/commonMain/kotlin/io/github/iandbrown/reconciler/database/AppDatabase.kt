package io.github.iandbrown.reconciler.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

private const val version = 6
private const val majorVersion = 1

@Database(
    entities = [
        Account::class,
        AccountGroup::class,
        AccountImportDefinition::class,
        ImportDefinition::class,
        Rule::class,
        Transaction::class,
        TransactionCategory::class],
    views = [
        ImportDefinitionListView::class,
        TransactionListView::class],
    version = version,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getAccountDao(): AccountDao
    abstract fun getAccountGroupDao(): AccountGroupDao
    abstract fun getAccountImportDefinitionDao(): AccountImportDefinitionDao
    abstract fun getImportDefinitionDao(): ImportDefinitionDao
    abstract fun getImportDefinitionListViewDao(): ImportDefinitionListViewDao
    abstract fun getRuleDao(): RuleDao
    abstract fun getTransactionCategoryDao(): TransactionCategoryDao
    abstract fun getTransactionListViewDao(): TransactionListViewDao
    abstract fun getTransactionDao(): TransactionDao
}

const val dbFileName = "AccountsDb$majorVersion.db"
