package io.github.iandbrown.home_energy.repository

import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.Usage
import io.github.iandbrown.home_energy.database.UsageDao
import io.github.iandbrown.home_energy.networking.OctopusApi

private data class PeriodUsage (var total: Double = 0.0, var count: Int = 1)

class MeterRepository(
    private val api: OctopusApi,
    private val usageDao: UsageDao
) {
    suspend fun syncConsumption(meters: List<Meter>) {
        usageDao.deleteAll()
        val datePattern = "(\\d{4})(-)(\\d{2})(-)(\\d{2})(T)(\\d{2})(:)(\\d{2})".toRegex()
        for (meter in meters) {
            var url: String? = null
            val periodUsages = mutableMapOf<Triple<Short, Short, Short>, PeriodUsage>()

            println("Fetching consumption for ${meter.name} (${meter.meterPointAdminNumber})")
            do {
                val response = api.getConsumption(meter, url)
                response.results.forEach { dto ->
                    if (dto.consumption > 0.0) {
                        val startDateParts = datePattern.matchAt(dto.intervalStart, 0)?.groupValues!!
                        val endDateParts = datePattern.matchAt(dto.intervalEnd, 0)?.groupValues!!
                        val startInstant = toPeriod(startDateParts)
                        val endInstant = toPeriod(endDateParts)
                        val count = endInstant - startInstant

                        if (count > 1) {
                            println("Multiple intervals ${dto.intervalStart}   $count consumption ${dto.consumption}")
                        }
                        if (startDateParts[3] == "01" && startDateParts[5] == "01") {
                            println("Multi Consumption ${dto.intervalStart} ${meter.id} ${dto.consumption}")
                        }
                        for (period in startInstant..endInstant) {
                            periodUsages.compute(
                                Triple(startDateParts[3].toShort(), startDateParts[5].toShort(), period.toShort())
                            ) { _, v ->
                                val consumption = when (meter.electric) {
                                    true -> dto.consumption
                                    false -> dto.consumption * 14.4 / 1.261 // hack convert m3 to kw
                                }
                                if (v == null)
                                    PeriodUsage(consumption, 1)
                                else
                                    PeriodUsage(v.total + consumption, v.count + 1)
                            }
                        }
                    }
                }
                url = response.next
            } while (url != null)
            for ((key, value) in periodUsages) {
                usageDao.insert(
                    Usage(key.first, key.second, key.third, meter.id, value.total / value.count)
                )
            }
            println("${meter.name} added ${periodUsages.size} usages")
        }
    }
}

private fun toPeriod(parts: List<String>) : Short = (parts[7].toInt() * 2 + parts[9].toInt() / 30).toShort()
