package io.github.iandbrown.sportplanner.di

import androidx.room.RoomDatabase
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.ui.AssociationEditScreen
import io.github.iandbrown.sportplanner.ui.AssociationListScreen
import io.github.iandbrown.sportplanner.ui.AssociationViewModel
import io.github.iandbrown.sportplanner.ui.CompetitionEditScreen
import io.github.iandbrown.sportplanner.ui.CompetitionListScreen
import io.github.iandbrown.sportplanner.ui.CompetitionViewModel
import io.github.iandbrown.sportplanner.ui.CupFixtureScreen
import io.github.iandbrown.sportplanner.ui.CupFixtureTableScreen
import io.github.iandbrown.sportplanner.ui.FarAssociationEditScreen
import io.github.iandbrown.sportplanner.ui.FarAssociationListScreen
import io.github.iandbrown.sportplanner.ui.FarAssociationListViewModel
import io.github.iandbrown.sportplanner.ui.FarAssociationViewModel
import io.github.iandbrown.sportplanner.ui.FixtureDateScreen
import io.github.iandbrown.sportplanner.ui.FixtureScreen
import io.github.iandbrown.sportplanner.ui.FixtureTableScreen
import io.github.iandbrown.sportplanner.ui.HomeScreen
import io.github.iandbrown.sportplanner.ui.Route
import io.github.iandbrown.sportplanner.ui.SeasonBreakEditScreen
import io.github.iandbrown.sportplanner.ui.SeasonBreakListScreen
import io.github.iandbrown.sportplanner.ui.SeasonBreakViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompCupFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionRoundEditScreen
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionRoundListScreen
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionRoundViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCompetitionViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCupFixtureScreen
import io.github.iandbrown.sportplanner.ui.SeasonCupFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonCupSummaryViewModel
import io.github.iandbrown.sportplanner.ui.SeasonEditScreen
import io.github.iandbrown.sportplanner.ui.SeasonFixtureViewModel
import io.github.iandbrown.sportplanner.ui.SeasonLeagueTeamCategoryViewModel
import io.github.iandbrown.sportplanner.ui.SeasonLeagueTeamViewModel
import io.github.iandbrown.sportplanner.ui.SeasonListScreen
import io.github.iandbrown.sportplanner.ui.SeasonRoundViewModel
import io.github.iandbrown.sportplanner.ui.SeasonTeamByCategoryScreen
import io.github.iandbrown.sportplanner.ui.SeasonTeamCategoryScreen
import io.github.iandbrown.sportplanner.ui.SeasonTeamCategoryViewModel
import io.github.iandbrown.sportplanner.ui.SeasonTeamScreen
import io.github.iandbrown.sportplanner.ui.SeasonTeamViewModel
import io.github.iandbrown.sportplanner.ui.SeasonViewModel
import io.github.iandbrown.sportplanner.ui.SummaryCupFixtureScreen
import io.github.iandbrown.sportplanner.ui.SummaryFixtureScreen
import io.github.iandbrown.sportplanner.ui.TeamCategoryEditScreen
import io.github.iandbrown.sportplanner.ui.TeamCategoryListScreen
import io.github.iandbrown.sportplanner.ui.TeamCategoryViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.navigation3.navigation
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

@OptIn(KoinExperimentalAPI::class)
private val injectableModules = module {
    navigation<Route.Home> { HomeScreen() }
    navigation<Route.AssociationList> { AssociationListScreen() }
    navigation<Route.AssociationEdit> { key -> AssociationEditScreen(key.association) }
    navigation<Route.CompetitionList> { CompetitionListScreen() }
    navigation<Route.CompetitionEdit> { key -> CompetitionEditScreen(key.competition) }
    navigation<Route.FarAssociationList> { FarAssociationListScreen() }
    navigation<Route.FarAssociationEdit> { key -> FarAssociationEditScreen(key.farAssociation) }
    navigation<Route.SeasonList> { SeasonListScreen() }
    navigation<Route.SeasonEdit> { key -> SeasonEditScreen(key.season) }
    navigation<Route.SeasonBreakList> { key -> SeasonBreakListScreen(key.season) }
    navigation<Route.SeasonBreakEdit> { key -> SeasonBreakEditScreen(key.season, key.seasonBreak) }
    navigation<Route.LeagueFixtures> { FixtureScreen() }
    navigation<Route.LeagueFixturesSummary> { key -> SummaryFixtureScreen(key.season) }
    navigation<Route.LeagueFixturesDate> { key -> FixtureDateScreen(key.season) }
    navigation<Route.LeagueFixturesTable> { key -> FixtureTableScreen(key.season) }
    navigation<Route.CupFixtures> { CupFixtureScreen() }
    navigation<Route.CupFixturesSummary> { key -> SummaryCupFixtureScreen(key.season) }
    navigation<Route.CupFixturesTable> { key -> CupFixtureTableScreen(key.season) }
    navigation<Route.SeasonTeams> { key -> SeasonTeamScreen(key.param) }
    navigation<Route.SeasonTeamsByCategory> { key -> SeasonTeamByCategoryScreen(key.param) }
    navigation<Route.SeasonTeamCategory> { key -> SeasonTeamCategoryScreen(key.param) }
    navigation<Route.SeasonCompetitionRoundList> { key -> SeasonCompetitionRoundListScreen(key.param) }
    navigation<Route.SeasonCompetitionRoundEdit> { key -> SeasonCompetitionRoundEditScreen(key.param, key.competitionRound) }
    navigation<Route.SeasonCupFixtures> { key -> SeasonCupFixtureScreen(key.param, key.competitionRound) }
    navigation<Route.TeamCategoryList> { TeamCategoryListScreen() }
    navigation<Route.TeamCategoryEdit> { key -> TeamCategoryEditScreen(key.teamCategory) }

    viewModel { SeasonBreakViewModel(it.get(), get()) }
    viewModel { SeasonCompCupFixtureViewModel(it.get(), it.get(), get()) }
    viewModel { SeasonCompViewModel(get()) }
    viewModel { SeasonCompetitionRoundViewModel(it.get(), it.get(), get()) }
    viewModel { SeasonCompetitionViewModel(it.get(), it.get(), get()) }
    viewModel { SeasonCupFixtureViewModel(it.get(), get()) }
    viewModel { SeasonCupSummaryViewModel(it.get(), get()) }
    viewModel { SeasonFixtureViewModel(it.get(), get()) }
    viewModel { SeasonLeagueTeamCategoryViewModel(it.get(), get()) }
    viewModel { SeasonLeagueTeamViewModel(it.get(), get()) }
    viewModel { SeasonRoundViewModel(it.get(), get()) }
    viewModel { SeasonTeamCategoryViewModel(it.get(), it.get(), get()) }
    viewModel { SeasonTeamViewModel(it.get(), it.get(), get()) }
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
