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

private interface TariffResolver {
    fun get(period: Short) : Double
}

internal class MeterTariffsViewModel(val dao: MeterTariffDao) : ViewModel() {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.getAll() }

    fun getState() : StateFlow<ViewModelState<MeterTariff>> = readDelegate.uiState
}

internal class RawUsageViewModel(val dao: RawUsageDao) : ViewModel() {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.get() }

    fun getState() : StateFlow<ViewModelState<RawUsage>> = readDelegate.uiState
}

internal typealias MeterId = Short
internal typealias Month = Short
internal typealias Period = Short

private data class MeterMonthPeriod(val meterId: MeterId, val month: Month, val period: Period)
internal data class MeterMonth(val meterId: MeterId, val month: Month)
private data class PeriodUsage (var total: Double, var days : Set<Short>,  var count: Int = 1) {

    operator fun plus(other: PeriodUsage) = PeriodUsage(total + other.total, days + other.days, count + other.count)

    fun average() = total / count
}

internal class MonthlyStatistics {
    val monthlyBill = mutableMapOf<MeterMonth, Double>()
    val monthlyKWh = mutableMapOf<MeterMonth, Double>()

    constructor(rawUsage: List<RawUsage>, allMeterTariffs: List<MeterTariff>) {
        val periodUsages = mutableMapOf<MeterMonthPeriod, PeriodUsage>()

        // compute average usage by month and period for each meter
        rawUsage.forEach {
            val key = MeterMonthPeriod(it.meterId.toShort(), it.month, it.period)
            periodUsages.merge(key,
                PeriodUsage(it.averageConsumption, setOf(it.day)),
                PeriodUsage::plus)
        }

        // combine into month and meter
        var meterId : Short? = null
        var tariffResolver : TariffResolver? = null
        periodUsages.keys.toList().sortedWith(compareBy { it.meterId })
            .forEach { meterMonthPeriod ->
                if (tariffResolver == null || meterId != meterMonthPeriod.meterId) {
                    tariffResolver = getTariffResolver(allMeterTariffs, meterMonthPeriod.meterId.toInt())
                    meterId = meterMonthPeriod.meterId
                }
                val meterMonth = MeterMonth(meterMonthPeriod.meterId, meterMonthPeriod.month)
                val periodUsage = periodUsages[meterMonthPeriod]!!
                val avg = periodUsage.average()
                val days = periodUsage.days.size

                monthlyBill.merge(meterMonth, avg * tariffResolver.get(meterMonthPeriod.period) * days, Double::plus)
                monthlyKWh.merge(meterMonth, avg * days, Double::plus)
            }
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

        TrailingIconLazyVerticalGrid(paddingValues, 3 + meterState.values().size, 0) {
            viewTextItems(listOf("Month"))
            viewTextItems(meterState.values().map { it.name })
            viewTextItems(listOf("Total", "Balance"))

            for (i in MONTHS.indices) {
                val month = ((setting.startMonth + i) % MONTHS.size).toShort()
                val year = setting.targetYear + if (setting.startMonth + i < MONTHS.size) 0 else 1
                val days = YearMonth(year, month + 1).numberOfDays

                viewTextItems(listOf("${MONTHS[month.toInt()]} $year"))
                // add in standing charge and sum
                var total = 0.0
                meterState.values()
                    .forEach {
                        val key = MeterMonth(it.id.toShort(), (month + 1).toShort())
                        monthlyStatistics.monthlyBill.merge(key, it.standingCharge * days, Double::plus)
                        viewTextItems(listOf(billValue(monthlyStatistics.monthlyBill[key]!!, monthlyStatistics.monthlyKWh[key])))
                        total += monthlyStatistics.monthlyBill[key]!!
                    }

                viewTextItems(listOf(billValue(total), billValue(balance)))
                balance += total + setting.directDebitAmount
                grandTotal += total
            }


            viewTextItems(listOf("kWh") + monthlyStatistics.monthlyKWh
                .entries
                .groupBy({ it.key.meterId }) { it.value }
                .mapValues { (_, values) -> billValue(values.sum()) }
                .values)
            viewTextItems(listOf("", ""))

            viewTextItems(listOf("Total"))
            viewTextItems(meterState
                .values()
                .map {meter -> billValue(monthlyStatistics.monthlyBill.filterKeys { it.meterId == meter.id.toShort() }.values.sum()) })
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

private fun getTariffResolver(allMeterTariffs: List<MeterTariff>, meterId: Int) : TariffResolver {
    val meterTariffs = allMeterTariffs.filter { it.meterId == meterId }

    return when (meterTariffs.size) {
        0 -> object : TariffResolver {
            override fun get(period: Short): Double = 0.0
        }
        1 -> object : TariffResolver {
            override fun get(period: Short): Double = meterTariffs[0].tariff
        }
        else -> {
            val perPeriod = (0..47).associateWith { 0.0 }.toMutableMap()

            meterTariffs.forEach {
                val from = it.fromHour * 2 + it.fromPeriod
                val to = it.toHour * 2 + it.toPeriod
                for (i in from until to) {
                    perPeriod[i] = it.tariff
                }
            }
            object : TariffResolver {
                override fun get(period: Short): Double {
                    return perPeriod[period.toInt()] ?: 0.0
                }
            }
        }
    }
}
