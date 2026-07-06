package io.github.iandbrown.sportplanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val routeSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Route.Home::class)
        subclass(Route.AssociationList::class)
        subclass(Route.AssociationEdit::class)
        subclass(Route.CompetitionList::class)
        subclass(Route.CompetitionEdit::class)
        subclass(Route.FarAssociationList::class)
        subclass(Route.FarAssociationEdit::class)
        subclass(Route.SeasonList::class)
        subclass(Route.SeasonEdit::class)
        subclass(Route.SeasonBreakList::class)
        subclass(Route.SeasonBreakEdit::class)
        subclass(Route.SeasonCompetitionRoundList::class)
        subclass(Route.SeasonCompetitionRoundEdit::class)
        subclass(Route.SeasonCupFixtures::class)
        subclass(Route.LeagueFixtures::class)
        subclass(Route.LeagueFixturesSummary::class)
        subclass(Route.LeagueFixturesDate::class)
        subclass(Route.LeagueFixturesTable::class)
        subclass(Route.CupFixtures::class)
        subclass(Route.CupFixturesSummary::class)
        subclass(Route.CupFixturesTable::class)
        subclass(Route.SeasonTeams::class)
        subclass(Route.SeasonTeamsByCategory::class)
        subclass(Route.SeasonTeamCategory::class)
        subclass(Route.TeamCategoryList::class)
        subclass(Route.TeamCategoryEdit::class)
    }
}

private val navigationConfig = SavedStateConfiguration {
    serializersModule = routeSerializersModule
}

@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {

    val topLevelRoute = rememberSaveable {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(navigationConfig, key)
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>()
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}

class Navigator(val state: NavigationState){
    fun navigate(route: NavKey){
        if (route in state.backStacks.keys){
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack(){
        val currentStack = state.backStacks[state.topLevelRoute] ?:
        error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute){
            if (state.topLevelRoute != state.startRoute) {
                state.topLevelRoute = state.startRoute
            }
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
