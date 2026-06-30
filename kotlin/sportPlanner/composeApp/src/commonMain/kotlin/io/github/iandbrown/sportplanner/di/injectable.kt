package io.github.iandbrown.sportplanner.di

import androidx.room.RoomDatabase
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.ui.AssociationViewModel
import io.github.iandbrown.sportplanner.ui.CompetitionViewModel
import io.github.iandbrown.sportplanner.ui.FarAssociationListViewModel
import io.github.iandbrown.sportplanner.ui.FarAssociationViewModel
import io.github.iandbrown.sportplanner.ui.SeasonBreakViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCupSummaryViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionRoundViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompCupFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonRoundViewModel
import io.github.iandbrown.sportplanner.ui.SeasonFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonLeagueTeamCategoryViewModel
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
    viewModel { SeasonBreakViewModel(it.get()) }
    viewModel { SeasonCompCupFixtureViewModel(it.get(), it.get()) }
    viewModel { SeasonRoundViewModel(it.get()) }
    viewModel { SeasonCompViewModel(it.get()) }
    viewModel { SeasonCompetitionRoundViewModel(it.get(), it.get()) }
    viewModel { SeasonCompetitionViewModel(it.get(), it.get()) }
    viewModel { SeasonCupSummaryViewModel(it.get()) }
    viewModel { SeasonFixtureViewModel(it.get()) }
    viewModel { SeasonLeagueTeamCategoryViewModel(it.get()) }
    viewModel { SeasonLeagueTeamViewModel(it.get()) }
    viewModel { SeasonTeamCategoryViewModel(it.get(), it.get()) }
    viewModel { SeasonTeamViewModel(it.get(), it.get()) }
    viewModelOf(::AssociationViewModel)
    viewModelOf(::CompetitionViewModel)
    viewModelOf(::FarAssociationListViewModel)
    viewModelOf(::FarAssociationViewModel)
    viewModelOf(::SeasonCompViewModel)
    viewModelOf(::SeasonViewModel)
    viewModelOf(::TeamCategoryViewModel)

    // Provide DAOs
    single { get<AppDatabase>().getAssociationDao() }
    single { get<AppDatabase>().getCompetitionDao() }
    single { get<AppDatabase>().getFarAssociationDao() }
    single { get<AppDatabase>().getFarAssociationViewDao() }
    single { get<AppDatabase>().getSeasonBreakDao() }
    single { get<AppDatabase>().getSeasonCompRoundViewDao() }
    single { get<AppDatabase>().getSeasonCompViewDao() }
    single { get<AppDatabase>().getSeasonCompetitionDao() }
    single { get<AppDatabase>().getSeasonCompetitionRoundDao() }
    single { get<AppDatabase>().getSeasonCupFixtureDao() }
    single { get<AppDatabase>().getSeasonCompCupFixtureViewDao() }
    single { get<AppDatabase>().getSeasonCupFixtureViewDao() }
    single { get<AppDatabase>().getSeasonCupSummaryViewDao() }
    single { get<AppDatabase>().getSeasonDao() }
    single { get<AppDatabase>().getSeasonFixtureDao() }
    single { get<AppDatabase>().getSeasonFixtureViewDao() }
    single { get<AppDatabase>().getSeasonLeagueTeamCategoryDao() }
    single { get<AppDatabase>().getSeasonLeagueTeamViewDao() }
    single { get<AppDatabase>().getSeasonRoundDao() }
    single { get<AppDatabase>().getSeasonTeamCategoryDao() }
    single { get<AppDatabase>().getSeasonTeamDao() }
    single { get<AppDatabase>().getTeamCategoryDao() }
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
