package io.github.iandbrown.home_energy.repository

import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.Usage
import io.github.iandbrown.home_energy.database.UsageDao
import io.github.iandbrown.home_energy.networking.OctopusApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class PeriodUsage (var total: Double = 0.0, var count: Int = 1)

class MeterRepository(
    private val api: OctopusApi,
    private val usageDao: UsageDao
) {
    suspend fun syncConsumption(meters: List<Meter>) {
        usageDao.deleteAll()
        for (meter in meters) {
            var url: String? = null
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX]")
            val periodUsages = mutableMapOf<Triple<Short, Short, Short>, PeriodUsage>()
            do {
                val response = api.getConsumption(meter, url)
                response.results.forEach { dto ->
                    if (dto.consumption > 0.0) {
                        var startInstant = LocalDateTime.parse(dto.intervalStart, formatter)
                        val endInstant = LocalDateTime.parse(dto.intervalEnd, formatter)
                        var count = 1

                        while (startInstant.plusMinutes((30 * count).toLong()) < endInstant) {
                            ++count
                        }
                        if (count > 1) {
                            println("Multiple intervals ${dto.intervalStart}   $count consumption ${dto.consumption}")
                        }
                        repeat(count) {
                            val period =
                                (startInstant.hour * 2 + startInstant.minute / 30).toShort()
                            periodUsages.compute(
                                Triple(
                                    startInstant.monthValue.toShort(),
                                    startInstant.dayOfMonth.toShort(),
                                    period
                                )
                            ) { _, v ->
                                if (v == null)
                                    PeriodUsage(dto.consumption / count, 1)
                                else
                                    PeriodUsage(v.total + dto.consumption / count, v.count + 1)
                            }
                            startInstant = startInstant.plusMinutes(30)
                        }
                    }
                }
                url = response.next
            } while (url != null)
            for ((key, value) in periodUsages) {
                usageDao.insert(
                    Usage(key.first, key.second, key.third, meter.meterPointAdminNumber, value.total / value.count)
                )
            }
        }
    }
}
