package io.github.iandbrown.home_energy.di

import io.github.iandbrown.home_energy.database.AppDatabase
import io.github.iandbrown.home_energy.database.MeterDao
import io.github.iandbrown.home_energy.database.SettingDao
import io.github.iandbrown.home_energy.database.UsageDao
import io.github.iandbrown.home_energy.networking.OctopusApi
import io.github.iandbrown.home_energy.repository.MeterRepository
import io.github.iandbrown.home_energy.repository.SettingsRepository
import io.github.iandbrown.home_energy.ui.uiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    includes(uiModule)

    single { OctopusApi(get()) }
    single { MeterRepository(get(), get()) }
    single { SettingsRepository(get(), CoroutineScope(Dispatchers.Default + SupervisorJob())) }

    // Provide DAOs
    single<MeterDao> { get<AppDatabase>().getMeterDao() }
    single<UsageDao> { get<AppDatabase>().getUsageDao() }
    single<SettingDao> { get<AppDatabase>().getSettingDao() }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule())
    }
