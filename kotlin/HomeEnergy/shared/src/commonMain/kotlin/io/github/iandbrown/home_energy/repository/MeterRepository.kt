package io.github.iandbrown.home_energy.repository

import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.RawUsage
import io.github.iandbrown.home_energy.database.RawUsageDao
import io.github.iandbrown.home_energy.database.SettingDao
import io.github.iandbrown.home_energy.networking.OctopusApi

class MeterRepository(
    private val api: OctopusApi,
    private val rawUsageDao: RawUsageDao,
    private val settingDao: SettingDao
) {
    suspend fun syncConsumption(meters: List<Meter>) {
        rawUsageDao.deleteAll()
        val fromYear = settingDao.get()[0].fromYear
        val datePattern = "(\\d{4})(-)(\\d{2})(-)(\\d{2})(T)(\\d{2})(:)(\\d{2})".toRegex()
        for (meter in meters) {
            var url: String? = null
            var counter = 0

            println("Fetching consumption for ${meter.name} (${meter.meterPointAdminNumber})")
            do {
                val response = api.getConsumption(meter, url, fromYear)
                response.results.forEach { dto ->
                    if (dto.consumption > 0.0 && datePattern.containsMatchIn(dto.intervalStart)) {
                        val startDateParts = datePattern.matchAt(dto.intervalStart, 0)?.groupValues!!
                        val endDateParts = datePattern.matchAt(dto.intervalEnd, 0)?.groupValues!!
                        val startInstant = toPeriod(startDateParts)
                        val endInstant = toPeriod(endDateParts)
                        val count = endInstant - startInstant

                        if (count > 1) {
                            println("Multiple intervals ${dto.intervalStart}   $count consumption ${dto.consumption}")
                        }
                        val consumption = when (meter.electric) {
                            true -> dto.consumption
                            false -> dto.consumption * 40 * 1.02264 / 3.6 // average
                        }
                        for (period in startInstant..endInstant) {
                            val periodConsumption = consumption / count
                            val month = startDateParts[3].toShort()
                            val day = startDateParts[5].toShort()
                            val year = startDateParts[1].toShort()
                            rawUsageDao.insert(RawUsage(year, month, day, period.toShort(), meter.id, periodConsumption))
                            ++counter
                        }
                    } else if (dto.consumption > 0.0) {
                        println("Invalid date $dto")
                    }
                }
                url = response.next
            } while (url != null)
            println("${meter.name} added $counter usages")
        }
    }
}

private fun toPeriod(parts: List<String>) : Short = (parts[7].toInt() * 2 + parts[9].toInt() / 30).toShort()
