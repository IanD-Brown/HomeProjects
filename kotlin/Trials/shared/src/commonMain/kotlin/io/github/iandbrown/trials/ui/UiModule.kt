package io.github.iandbrown.trials.ui

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val uiModule = module {
    viewModelOf(::NominationViewModel)

    navigation<Route.Root> {
        val backstack = LocalBackstack.current
        RouteScreen(onNavigate = { backstack.add(it) })
    }

    navigation<Route.Nominations> {
        NominationList()
    }
}
