package io.github.iandbrown.trials.ui

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val uiModule = module {
    viewModelOf(::EmailTemplateViewModel)
    viewModelOf(::NominationViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::StateViewModel)
    viewModelOf(::TeamCategoryViewModel)
    viewModelOf(::TrialViewModel)

    navigation<Route.Root> {
        val backstack = LocalBackstack.current
        RouteScreen(onNavigate = { backstack.add(it) })
    }

    navigation<Route.Nominations> {
        val backstack = LocalBackstack.current
        NominationList { backstack.add(Route.NominationEditor(it)) }
    }

    navigation<Route.NominationEditor> {
        val backstack = LocalBackstack.current
        NominationEditor(it.entity) { backstack.removeLastOrNull() }
    }

    navigation<Route.SettingView> {
        val backstack = LocalBackstack.current
        SettingsEditorView { backstack.removeLastOrNull() }
    }

    navigation<Route.EmailTemplates> {
        val backstack = LocalBackstack.current
        EmailTemplateView {backstack.add(Route.EmailTemplateEditor(it))}
    }

    navigation<Route.EmailTemplateEditor> {
        val backstack = LocalBackstack.current
        EmailTemplateEditor(it.entity) { backstack.removeLastOrNull() }
    }

    navigation<Route.States> {
        val backstack = LocalBackstack.current
        StateView { s, n -> backstack.add(Route.StateEditor(s, n)) }
    }

    navigation<Route.StateEditor> { route ->
        val backstack = LocalBackstack.current
        StateEditor(route.entity, route.currentNextStates) {
            backstack.removeLastOrNull()
        }
    }

    navigation<Route.TeamCategories> {
        TeamCategoryView()
    }
}
