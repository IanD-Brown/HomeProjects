package io.github.iandbrown.sportplanner.di

import io.github.iandbrown.sportplanner.ui.TeamCategoryViewModel
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.DBFactory
import io.github.iandbrown.sportplanner.ui.AssociationViewModel
import io.github.iandbrown.sportplanner.ui.CompetitionViewModel
import io.github.iandbrown.sportplanner.ui.SeasonBreakViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionRoundViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionViewModel
import io.github.iandbrown.sportplanner.ui.SeasonFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonTeamCategoryViewModel
import io.github.iandbrown.sportplanner.ui.SeasonTeamViewModel
import io.github.iandbrown.sportplanner.ui.SeasonViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val injectableModules = module {
    viewModelOf(::AssociationViewModel)
    viewModelOf(::CompetitionViewModel)
    viewModelOf(::SeasonBreakViewModel)
    viewModelOf(::SeasonCompetitionRoundViewModel)
    viewModelOf(::SeasonCompetitionViewModel)
    viewModelOf(::SeasonCompViewModel)
    viewModel {parameters -> SeasonFixtureViewModel(parameters.get()) }
    viewModelOf(::SeasonTeamCategoryViewModel)
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
