package org.idb.di

import AssociationViewModel
import org.idb.database.AppDatabase
import org.idb.database.DBFactory
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val injectableModules = module {
    viewModelOf(::AssociationViewModel)
}

fun startKoinCommon(dbFactory : DBFactory,
                    appDeclaration: KoinAppDeclaration = {}) {
    val dataModule = module {
        // Database
        single<AppDatabase> { dbFactory.createDatabase() }
    }
    startKoin {
        appDeclaration()
        modules(injectableModules, dataModule)
    }
}
