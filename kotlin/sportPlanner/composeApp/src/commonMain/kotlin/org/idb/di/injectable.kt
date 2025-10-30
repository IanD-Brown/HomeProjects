package org.idb.di

import org.idb.ui.TeamCategoryViewModel
import org.idb.database.AppDatabase
import org.idb.database.DBFactory
import org.idb.ui.AssociationViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val injectableModules = module {
    viewModelOf(::AssociationViewModel)
    viewModelOf(::TeamCategoryViewModel)
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
