package io.github.iandbrown.reconciler.di

import androidx.room.RoomDatabase
import io.github.iandbrown.reconciler.database.AppDatabase
import io.github.iandbrown.reconciler.logic.PDFConverterInterface
import io.github.iandbrown.reconciler.ui.AccountGroupViewModel
import io.github.iandbrown.reconciler.ui.AccountViewModel
import io.github.iandbrown.reconciler.ui.ImportDefinitionListViewModel
import io.github.iandbrown.reconciler.ui.ImportDefinitionViewModel
import io.github.iandbrown.reconciler.ui.RuleViewModel
import io.github.iandbrown.reconciler.ui.TransactionCategoryViewModel
import io.github.iandbrown.reconciler.ui.TransactionListViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

// Helper to use Koin inject in top-level functions
inline fun <reified T : Any> inject() = lazy { getKoin().get<T>() }

private val injectableModules = module {
    viewModelOf(::AccountGroupViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::ImportDefinitionListViewModel)
    viewModelOf(::ImportDefinitionViewModel)
    viewModelOf(::RuleViewModel)
    viewModelOf(::TransactionCategoryViewModel)
    viewModelOf(::TransactionListViewModel)

    // Provide DAOs
    single { get<AppDatabase>().getAccountDao() }
    single { get<AppDatabase>().getAccountGroupDao() }
    single { get<AppDatabase>().getAccountImportDefinitionDao() }
    single { get<AppDatabase>().getImportDefinitionDao() }
    single { get<AppDatabase>().getImportDefinitionListViewDao() }
    single { get<AppDatabase>().getRuleDao() }
    single { get<AppDatabase>().getTransactionCategoryDao() }
    single { get<AppDatabase>().getTransactionDao() }
    single { get<AppDatabase>().getTransactionListViewDao() }
}

internal lateinit var koinApp : KoinApplication

fun startKoinCommon(databaseBuilder: RoomDatabase.Builder<AppDatabase>,
                    pdfConverterBuilder: (ByteArray) -> PDFConverterInterface,
                    appDeclaration: KoinAppDeclaration = {}) {
    val platformModule = module {
        // Database
        single<AppDatabase> { databaseBuilder.build() }
        single<PDFConverterInterface> { (source: ByteArray) -> pdfConverterBuilder(source) }
    }
    koinApp = startKoin {
        modules(injectableModules, platformModule)
        appDeclaration()
    }
}
