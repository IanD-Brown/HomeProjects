package io.github.iandbrown.sportplanner.di

import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.DBFactory
import io.github.iandbrown.sportplanner.ui.AssociationViewModel
import io.github.iandbrown.sportplanner.ui.CompetitionViewModel
import io.github.iandbrown.sportplanner.ui.SeasonBreakViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionRoundViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCupFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonLeagueTeamViewModel
import io.github.iandbrown.sportplanner.ui.SeasonTeamCategoryViewModel
import io.github.iandbrown.sportplanner.ui.SeasonTeamViewModel
import io.github.iandbrown.sportplanner.ui.SeasonViewModel
import io.github.iandbrown.sportplanner.ui.TeamCategoryViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

// Helper to use Koin inject in top-level functions
inline fun <reified T : Any> inject() = lazy { getKoin().get<T>() }

private val injectableModules = module {
    viewModelOf(::AssociationViewModel)
    viewModelOf(::CompetitionViewModel)
    viewModel {parameters -> SeasonBreakViewModel(parameters.get()) }
    viewModel { p -> SeasonCompetitionRoundViewModel(p.get(), p.get()) }
    viewModel {p -> SeasonCompetitionViewModel(p.get(), p.get()) }
    viewModelOf(::SeasonCompViewModel)
    viewModel {p -> SeasonCupFixtureViewModel(p.get(), p.get()) }
    viewModel {p -> SeasonFixtureViewModel(p.get()) }
    viewModel {p -> SeasonTeamCategoryViewModel(p.get(), p.get()) }
    viewModel {p -> SeasonTeamViewModel(p.get(), p.get()) }
    viewModelOf(::SeasonViewModel)
    viewModelOf(::TeamCategoryViewModel)
    viewModel {p -> SeasonLeagueTeamViewModel(p.get()) }

    // Provide DAOs
    single { get<AppDatabase>().getAssociationDao() }
    single { get<AppDatabase>().getCompetitionDao() }
    single { get<AppDatabase>().getSeasonBreakDao() }
    single { get<AppDatabase>().getSeasonCompViewDao() }
    single { get<AppDatabase>().getSeasonCompetitionDao() }
    single { get<AppDatabase>().getSeasonCompetitionRoundDao() }
    single { get<AppDatabase>().getSeasonCompRoundViewDao() }
    single { get<AppDatabase>().getSeasonCupFixtureDao() }
    single { get<AppDatabase>().getSeasonCupFixtureViewDao() }
    single { get<AppDatabase>().getSeasonDao() }
    single { get<AppDatabase>().getSeasonFixtureDao() }
    single { get<AppDatabase>().getSeasonFixtureViewDao() }
    single { get<AppDatabase>().getSeasonTeamCategoryDao() }
    single { get<AppDatabase>().getSeasonTeamDao() }
    single { get<AppDatabase>().getTeamCategoryDao() }
    single { get<AppDatabase>().getSeasonLeagueTeamViewDao() }
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
