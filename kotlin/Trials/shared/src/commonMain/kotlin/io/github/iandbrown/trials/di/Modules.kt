package io.github.iandbrown.trials.di

import io.github.iandbrown.trials.database.AppDatabase
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.ui.uiModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    includes(uiModule)

    // Provide DAOs
    single<NominationDao> { get<AppDatabase>().getNominationDao() }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule())
    }
