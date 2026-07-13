package io.github.iandbrown.home_energy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.home_energy.database.MeterTariff
import io.github.iandbrown.home_energy.database.MeterTariffDao
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

@Composable
internal fun FutureScreen() {
    val usageViewModel: UsageViewModel = koinViewModel()
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
        val monthlyBill = mutableMapOf<Pair<Int, Short>, Double>()
        var tariffResolver : TariffResolver? = null
        var meterId : Int? = null

        usageState.values()
            .sortedWith(compareBy({ it.meterId }, { it.month }))
            .forEach {
                if (tariffResolver == null || meterId != it.meterId) {
                    tariffResolver = getTariffResolver(tariffState.values(), it.meterId)
                    meterId = it.meterId
                }
                val key = Pair(meterId, (it.month - 1).toShort())
                monthlyBill.merge(key, it.averageConsumption * tariffResolver.get(it.period), Double::plus)
            }
        println(monthlyBill)

        TrailingIconLazyVerticalGrid(paddingValues, 3 + meterState.values().size, 0) {
            viewTextItems(listOf("Month"))
            viewTextItems(meterState.values().map { it.name })
            viewTextItems(listOf("Total", "Balance"))

            for (i in 0..11) {
                val month = ((setting.startMonth + i) % 12).toShort()
                val year = setting.targetYear + if (setting.startMonth + i < 12) 0 else 1
                val days = YearMonth(year, month + 1).numberOfDays

                viewTextItems(listOf("${MONTHS[month.toInt()]} $year"))
                // add in standing charge and sum
                var total = 0.0
                meterState.values()
                    .forEach {
                        val key = Pair(it.id, month)
                        monthlyBill.merge(key, it.standingCharge * days, Double::plus)
                        viewTextItems(listOf(billValue(monthlyBill[key]!!)))
                        total += monthlyBill[key]!!
                    }

                viewTextItems(listOf(billValue(total), billValue(balance)))
                balance += total + setting.directDebitAmount
            }
        }
    }
}

private fun billValue(amount: Double) : String = String.format(Locale.UK, "%.2f", amount)

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
