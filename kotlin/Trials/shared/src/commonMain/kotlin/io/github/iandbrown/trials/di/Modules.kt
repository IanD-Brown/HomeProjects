package io.github.iandbrown.trials.di

import io.github.iandbrown.trials.database.AdultDetailsDao
import io.github.iandbrown.trials.database.AppDatabase
import io.github.iandbrown.trials.database.EmailTemplateDao
import io.github.iandbrown.trials.database.NextStateDao
import io.github.iandbrown.trials.database.NominationAdultDetailsDao
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.database.Setting
import io.github.iandbrown.trials.database.SettingDao
import io.github.iandbrown.trials.database.StateDao
import io.github.iandbrown.trials.database.StateWithNextStatesDao
import io.github.iandbrown.trials.database.TeamCategoryDao
import io.github.iandbrown.trials.database.TrialDao
import io.github.iandbrown.trials.database.TrialDetailsViewDao
import io.github.iandbrown.trials.database.TrialsViewDao
import io.github.iandbrown.trials.logic.EmailSender
import io.github.iandbrown.trials.ui.uiModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    includes(uiModule)

    // Provide DAOs
    single<AdultDetailsDao> { get<AppDatabase>().getAdultDetailsDao() }
    single<EmailTemplateDao> { get<AppDatabase>().getEmailTemplateDao() }
    single<NextStateDao> { get<AppDatabase>().getNextStateDao() }
    single<NominationAdultDetailsDao> { get<AppDatabase>().getNominationAdultDetailsDao() }
    single<NominationDao> { get<AppDatabase>().getNominationDao() }
    single<SettingDao> { get<AppDatabase>().getSettingDao() }
    single<StateDao> { get<AppDatabase>().getStateDao() }
    single<StateWithNextStatesDao> { get<AppDatabase>().getStateWithNextStatesDao() }
    single<TeamCategoryDao> { get<AppDatabase>().getTeamCategoryDao() }
    single<TrialDao> { get<AppDatabase>().getTrialDao() }
    single<TrialDetailsViewDao> { get<AppDatabase>().getTrialDetailsViewDao() }
    single<TrialsViewDao> { get<AppDatabase>().getTrialsViewDao() }

    // EmailSender taking Setting
    factory { (setting: Setting) -> EmailSender(setting) }
}

expect fun platformModule(): Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule())
    }
