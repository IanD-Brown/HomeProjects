package io.github.iandbrown.home_energy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.home_energy.database.MeterTariff
import io.github.iandbrown.home_energy.database.MeterTariffDao
import io.github.iandbrown.home_energy.database.RawUsage
import io.github.iandbrown.home_energy.database.RawUsageDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.YearMonth
import org.koin.compose.viewmodel.koinViewModel
import java.util.Locale
import kotlin.collections.forEach

private typealias DayPeriod = Short
internal typealias MeterId = Short
internal typealias Month = Short

internal class MeterTariffsViewModel(val dao: MeterTariffDao) : ViewModel() {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.getAll() }

    fun getState() : StateFlow<ViewModelState<MeterTariff>> = readDelegate.uiState
}

internal class RawUsageViewModel(val dao: RawUsageDao) : ViewModel() {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.get() }

    fun getState() : StateFlow<ViewModelState<RawUsage>> = readDelegate.uiState
}

internal data class MeterMonth(val meterId: MeterId, val month: Month)

private data class MeterMonthUsage(val kiloWatts: Double, val pounds: Double, val dayCount: Int) {
    operator fun plus(other: MeterMonthUsage) = MeterMonthUsage(
        kiloWatts + other.kiloWatts,
        pounds + other.pounds,
        dayCount + other.dayCount)

    fun add(kwn: Double, cost: Double, days: Int = 0) = MeterMonthUsage(kiloWatts + kwn, pounds + cost, dayCount + days)

    fun averageKwh() = kiloWatts / dayCount

    fun averagePounds() = pounds / dayCount
}

internal class MonthlyStatistics {
    private val meterMonthUsage: Map<MeterMonth, MeterMonthUsage>

    constructor(rawUsage: List<RawUsage>, allMeterTariffs: List<MeterTariff>) {
        val periodToPriceByMeter = mutableMapOf<DayPeriod, MutableMap<MeterId, Double>>()
        allMeterTariffs.forEach {
            for (period in toDayPeriod(it.fromHour, it.fromPeriod) until toDayPeriod(it.toHour, it.toPeriod)) {
                val priceMap = periodToPriceByMeter.getOrPut(period.toShort()) { mutableMapOf() }
                priceMap[it.meterId.toShort()] = it.tariff
            }
        }
        val usageByMeterMonth = mutableMapOf<MeterMonth, MeterMonthUsage>()
        var currentMeterId : MeterId? = null
        var currentMonth : Month? = null
        var currentDay: Short? = null
        var currentMeterMonthUsage: MeterMonthUsage? = null
        rawUsage
            .sortedWith(compareBy<RawUsage>{it.meterId}.thenBy { it.year }.thenBy { it.month }.thenBy { it.day })
            .forEach {
                if (it.meterId.toShort() != currentMeterId || it.month != currentMonth) {
                    if (currentMeterMonthUsage != null && currentMeterId != null) {
                        usageByMeterMonth.merge(MeterMonth(currentMeterId, currentMonth!!), currentMeterMonthUsage, MeterMonthUsage::plus)
                    }
                    currentMeterId = it.meterId.toShort()
                    currentMonth = it.month
                    currentMeterMonthUsage = MeterMonthUsage(0.0, 0.0, 0)
                }
                val cost = it.averageConsumption * (periodToPriceByMeter[it.period]?.get(currentMeterId) ?: 0.0)
                if (it.day != currentDay) {
                    currentDay = it.day
                    currentMeterMonthUsage = currentMeterMonthUsage!!.add(it.averageConsumption, cost, 1)
                } else {
                    currentMeterMonthUsage = currentMeterMonthUsage!!.add(it.averageConsumption, cost)
                }
            }
        meterMonthUsage = usageByMeterMonth
    }

    private fun toDayPeriod(hour: Short, period: Short) = hour * 2 + period

    fun getMonthlyKWh(meterMonth: MeterMonth, year: Int) : Double {
        val days = YearMonth(year, meterMonth.month.toInt()).numberOfDays

        return (meterMonthUsage[meterMonth]?.averageKwh()?.times(days) ?: 0.0)
    }

    fun getMonthlyBill(meterMonth: MeterMonth, year: Int, standingCharge: Double) : Double {
        val days = YearMonth(year, meterMonth.month.toInt()).numberOfDays
        return (meterMonthUsage[meterMonth]?.averagePounds()?.times(days) ?: 0.0) + standingCharge * days
    }
}

@Composable
internal fun FutureScreen() {
    val usageViewModel: RawUsageViewModel = koinViewModel()
    val usageState by usageViewModel.getState().collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.getState().collectAsState()
    val meterViewModel: MeterViewModel = koinViewModel()
    val meterState by meterViewModel.getState().collectAsState()
    val tariffViewModel: MeterTariffsViewModel = koinViewModel()
    val tariffState by tariffViewModel.getState().collectAsState()

    ViewCommon("Future Prediction",
        persistentListOf(usageState, settingsState, meterState, tariffState),) { paddingValues ->
        val setting = settingsState.values()[0]
        var balance = setting.initialBalance
        val monthlyStatistics = MonthlyStatistics(usageState.values(), tariffState.values())
        var grandTotal = 0.0
        val meterTotalKWh = mutableMapOf<MeterId, Double>()
        val meterTotalBill = mutableMapOf<MeterId, Double>()

        TrailingIconLazyVerticalGrid(paddingValues, 3 + meterState.values().size, 0) {
            viewTextItems(listOf("Month"))
            viewTextItems(meterState.values().map { it.name })
            viewTextItems(listOf("Total", "Balance"))

            for (i in MONTHS.indices) {
                val month = ((setting.startMonth + i) % MONTHS.size).toShort()
                val year = setting.targetYear + if (setting.startMonth + i < MONTHS.size) 0 else 1

                viewTextItems(listOf("${MONTHS[month.toInt()]} $year"))
                // add in standing charge and sum
                var total = 0.0
                meterState.values()
                    .forEach {
                        val meterMonth = MeterMonth(it.id.toShort(), (month + 1).toShort())
                        val monthlyBill = monthlyStatistics.getMonthlyBill(meterMonth, year, it.standingCharge)
                        val monthlyKWh = monthlyStatistics.getMonthlyKWh(meterMonth, year)
                        viewTextItems(listOf(billValue(monthlyBill, monthlyKWh)))
                        total += monthlyBill
                        meterTotalKWh.merge(it.id.toShort(), monthlyKWh, Double::plus)
                        meterTotalBill.merge(it.id.toShort(), monthlyBill, Double::plus)
                    }

                viewTextItems(listOf(billValue(total), billValue(balance)))
                balance += total + setting.directDebitAmount
                grandTotal += total
            }

            viewTextItems(listOf("kWh") + meterState.values().map { billValue(meterTotalKWh[it.id.toShort()] ?: 0.0)})
            viewTextItems(listOf("", ""))

            viewTextItems(listOf("Total"))
            viewTextItems(meterState.values().map { billValue(meterTotalBill[it.id.toShort()] ?: 0.0)})
            viewTextItems(listOf(billValue(grandTotal), ""))
        }
    }
}

private fun billValue(amount: Double, kWh: Double? = null) : String {
    if (kWh != null) {
        return "£${String.format(Locale.UK, "%.2f", amount)}(${String.format(Locale.UK, "%.2f", kWh)})"
    }
    return String.format(Locale.UK, "%.2f", amount)
}
