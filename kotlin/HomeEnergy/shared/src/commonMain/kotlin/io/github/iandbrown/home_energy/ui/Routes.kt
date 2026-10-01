package io.github.iandbrown.home_energy.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.MeterTariff
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Root : Route
    @Serializable
    data object Meters : Route
    @Serializable
    data object Future : Route
    @Serializable
    data object SettingView : Route
    @Serializable
    data class MeterEditor(val meter: Meter? = null) : Route
    @Serializable
    data class MeterTariffList(val meter: Meter) : Route
    @Serializable
    data class MeterTariffEditor(val meterId: Int, val meterTariff: MeterTariff?) : Route
}

val LocalBackstack = compositionLocalOf<SnapshotStateList<Any>> { error("No backstack provided") }
