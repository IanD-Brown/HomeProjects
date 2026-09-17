package io.github.iandbrown.trials.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Route {
    @Serializable
    data object Root : Route
    @Serializable
    data object Nominations : Route
}

val LocalBackstack = compositionLocalOf<SnapshotStateList<Any>> { error("No backstack provided") }
