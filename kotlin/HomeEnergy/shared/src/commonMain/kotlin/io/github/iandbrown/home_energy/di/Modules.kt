package io.github.iandbrown.home_energy.di

import io.github.iandbrown.home_energy.database.AppDatabase
import io.github.iandbrown.home_energy.database.MeterDao
import io.github.iandbrown.home_energy.database.UsageDao
import io.github.iandbrown.home_energy.ui.MeterViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    viewModelOf(::MeterViewModel)

    // Provide DAOs
    single<MeterDao> { get<AppDatabase>().getMeterDao() }
    single<UsageDao> { get<AppDatabase>().getUsageDao() }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule())
    }
