package org.idb.di

import org.idb.ui.TeamCategoryViewModel
import org.idb.database.AppDatabase
import org.idb.database.DBFactory
import org.idb.ui.AssociationViewModel
import org.idb.ui.CompetitionViewModel
import org.idb.ui.SeasonTeamViewModel
import org.idb.ui.SeasonViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val injectableModules = module {
    viewModelOf(::AssociationViewModel)
    viewModelOf(::CompetitionViewModel)
    viewModelOf(::SeasonTeamViewModel)
    viewModelOf(::SeasonViewModel)
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
