package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.lazy.grid.GridItemSpan
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

private data class InterimKey(val meterId: MeterId, val year : Short, val month : Short)

private data class InterimMeterMonthUsage(val meterId: MeterId = 0, val year : Short = 0, val month : Short = 0, val kwh: Double = 0.0, val cost: Double = 0.0) {
    fun plus(rawUsage: RawUsage, cost: Double) : InterimMeterMonthUsage {
        return InterimMeterMonthUsage(rawUsage.meterId.toShort(), rawUsage.year, rawUsage.month, kwh + rawUsage.averageConsumption, cost + cost)
    }
}

private class ValuePair(val kwh: Double, val cost: Double, val count: Int = 1) {
    fun plus(k: Double, c: Double) : ValuePair = ValuePair(kwh + k, cost + c)

    fun acc(other: ValuePair) : ValuePair = ValuePair(kwh + other.kwh, cost + other.cost, other.count + 1)

    fun averageKwh() = kwh / count

    fun averagePounds() = cost / count
}

internal class MonthlyStatistics {
    private val meterMonthUsage: Map<MeterMonth, ValuePair>

    constructor(rawUsage: List<RawUsage>, allMeterTariffs: List<MeterTariff>) {
        val periodToPriceByMeter = mutableMapOf<DayPeriod, MutableMap<MeterId, Double>>()
        allMeterTariffs.forEach {
            for (period in toDayPeriod(it.fromHour, it.fromPeriod) until toDayPeriod(it.toHour, it.toPeriod)) {
                val priceMap = periodToPriceByMeter.getOrPut(period.toShort()) { mutableMapOf() }
                priceMap[it.meterId.toShort()] = it.tariff
            }
        }
        val interim = rawUsage
            .groupBy { InterimKey(it.meterId.toShort(), it.year, it.month) }
            .mapValues { (_, items) ->
                items.fold(ValuePair(0.0, 0.0)) {acc, rawUsage ->
                    acc.plus(rawUsage.averageConsumption,
                        rawUsage.averageConsumption * (periodToPriceByMeter[rawUsage.period]?.get(rawUsage.meterId.toShort()) ?: 0.0))
                }
            }
        meterMonthUsage = interim.keys.groupBy { MeterMonth(it.meterId, it.month) }
            .mapValues { (key, items) ->
                items.map { interim[it]!! }
                .reduce { acc, item -> item.acc(acc) }
            }
    }

    private fun toDayPeriod(hour: Short, period: Short) = hour * 2 + period

    fun getMonthlyKWh(meterMonth: MeterMonth) : Double {
        return (meterMonthUsage[meterMonth]?.averageKwh() ?: 0.0)
    }

    fun getMonthlyBill(meterMonth: MeterMonth, year: Int, standingCharge: Double) : Double {
        val days = YearMonth(year, meterMonth.month.toInt()).numberOfDays
        return (meterMonthUsage[meterMonth]?.averagePounds() ?: 0.0) + standingCharge * days
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
        val monthlyStatistics = MonthlyStatistics(usageState.values(), tariffState.values().filter { it.activeAccount })
        var grandTotal = 0.0
        var compareGrandTotal = 0.0
        val meterTotalKWh = mutableMapOf<MeterId, Double>()
        val meterTotalBill = mutableMapOf<MeterId, Double>()
        val compareTariffs = tariffState.values().filter { !it.activeAccount }
        val compareStatistics = if (compareTariffs.isNotEmpty()) {
            MonthlyStatistics(usageState.values(), compareTariffs)
        } else {
            null
        }
        val meterTotalCompareBill = mutableMapOf<MeterId, Double>()

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
                var compareTotal = 0.0
                meterState.values()
                    .forEach {
                        val meterMonth = MeterMonth(it.id.toShort(), (month + 1).toShort())
                        val monthlyBill = monthlyStatistics.getMonthlyBill(meterMonth, year, it.standingCharge)
                        val monthlyKWh = monthlyStatistics.getMonthlyKWh(meterMonth)
                        viewTextItems(listOf(billValue(monthlyBill, monthlyKWh)))
                        total += monthlyBill
                        meterTotalKWh.merge(it.id.toShort(), monthlyKWh, Double::plus)
                        meterTotalBill.merge(it.id.toShort(), monthlyBill, Double::plus)
                        if (compareStatistics != null) {
                            val compareMonthlyBill = compareStatistics.getMonthlyBill(meterMonth, year, it.compareStandingCharge)
                            meterTotalCompareBill.merge(it.id.toShort(), compareMonthlyBill, Double::plus)
                            compareTotal += compareMonthlyBill
                        }
                    }

                viewTextItems(listOf(billValue(total), billValue(balance)))
                balance += total + setting.directDebitAmount
                grandTotal += total
                compareGrandTotal += compareTotal
            }

            item(span = { GridItemSpan(3 + meterState.values().size) }) {ViewText("")}

            viewTextItems(listOf("Total"))
            viewTextItems(meterState.values().map { billValue(meterTotalBill[it.id.toShort()] ?: 0.0, meterTotalKWh[it.id.toShort()] ?: 0.0)})
            viewTextItems(listOf(billValue(grandTotal), ""))

            if (compareStatistics != null) {
                item(span = { GridItemSpan(3 + meterState.values().size) }) {ViewText("")}
                viewTextItems(listOf("Compare"))
                viewTextItems(meterState.values().map { billValue(meterTotalCompareBill[it.id.toShort()] ?: 0.0) })
                viewTextItems(listOf(billValue(compareGrandTotal), ""))
            }
        }
    }
}

private fun billValue(amount: Double, kWh: Double? = null) : String {
    if (kWh != null) {
        return "£${String.format(Locale.UK, "%.2f", amount)}(${String.format(Locale.UK, "%.2f", kWh)})"
    }
    return String.format(Locale.UK, "£% .2f", amount)
}
