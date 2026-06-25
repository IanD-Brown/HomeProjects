package io.github.iandbrown.home_energy.di

import androidx.room.RoomDatabase
import io.github.iandbrown.home_energy.database.AppDatabase
import io.github.iandbrown.home_energy.database.MeterDao
import io.github.iandbrown.home_energy.ui.MeterViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import org.koin.plugin.module.dsl.viewModel

// Helper to use Koin inject in top-level functions
inline fun <reified T : Any> inject() = getKoin().get<T>()

private val injectableModules = module {
    viewModel<MeterViewModel>()

    // Provide DAOs
    single<MeterDao> { get<AppDatabase>().getMeterDao() }
}

fun startKoinCommon(databaseBuilder: RoomDatabase.Builder<AppDatabase>,
                    appDeclaration: KoinAppDeclaration = {}) {
    val dataModule = module {
        // Database
        single<AppDatabase> { databaseBuilder.build() }
    }
    startKoin {
        appDeclaration()
        modules(injectableModules, dataModule)
    }
}
