package io.github.iandbrown.home_energy.ui

import io.github.iandbrown.home_energy.database.Meter
import kotlinx.serialization.Serializable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList

@Serializable
sealed interface Route {
    @Serializable
    data object Root : Route
    @Serializable
    data object Meters : Route
    @Serializable
    data object Usage : Route
    @Serializable
    data class MeterEditor(val meter: Meter? = null) : Route
}

val LocalBackstack = compositionLocalOf<SnapshotStateList<Any>> { error("No backstack provided") }
