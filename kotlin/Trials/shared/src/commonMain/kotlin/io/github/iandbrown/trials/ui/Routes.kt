package io.github.iandbrown.trials.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.iandbrown.trials.database.State
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Route {
    @Serializable
    data object Root : Route
    @Serializable
    data object Nominations : Route
    @Serializable
    data object SettingView : Route
    @Serializable
    data object EmailTemplates : Route
    @Serializable
    data object States : Route
    @Serializable
    data object TeamCategories : Route

    @Serializable
    data class StateEditor(val entity: State?, val currentNextStates: ImmutableSet<String>) : Route
}

val LocalBackstack = compositionLocalOf<SnapshotStateList<Any>> { error("No backstack provided") }
