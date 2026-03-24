package io.github.iandbrown.reconciler.di

import androidx.room.RoomDatabase
import io.github.iandbrown.reconciler.database.AppDatabase
import io.github.iandbrown.reconciler.ui.AccountViewModel
import io.github.iandbrown.reconciler.ui.ImportDefinitionListViewModel
import io.github.iandbrown.reconciler.ui.ImportDefinitionViewModel
import io.github.iandbrown.reconciler.ui.RuleViewModel
import io.github.iandbrown.reconciler.ui.TransactionCategoryViewModel
import io.github.iandbrown.reconciler.ui.TransactionViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

// Helper to use Koin inject in top-level functions
inline fun <reified T : Any> inject() = lazy { getKoin().get<T>() }

private val injectableModules = module {
    viewModelOf(::AccountViewModel)
    viewModelOf(::ImportDefinitionListViewModel)
    viewModelOf(::ImportDefinitionViewModel)
    viewModelOf(::RuleViewModel)
    viewModelOf(::TransactionCategoryViewModel)
    viewModelOf(::TransactionViewModel)

    // Provide DAOs
    single { get<AppDatabase>().getAccountDao() }
    single { get<AppDatabase>().getAccountImportDefinitionDao() }
    single { get<AppDatabase>().getImportDefinitionDao() }
    single { get<AppDatabase>().getImportDefinitionListViewDao() }
    single { get<AppDatabase>().getRuleDao() }
    single { get<AppDatabase>().getTransactionCategoryDao() }
    single { get<AppDatabase>().getTransactionDao() }
}

fun startKoinCommon(databaseBuilder: RoomDatabase.Builder<AppDatabase>,
                    appDeclaration: KoinAppDeclaration = {}) {
    val dataModule = module {
        // Database
        single<AppDatabase> { databaseBuilder.build() }
    }
    startKoin {
        modules(injectableModules, dataModule)
        appDeclaration()
    }
}
