package io.github.iandbrown.home_energy.repository

import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.Usage
import io.github.iandbrown.home_energy.database.UsageDao
import io.github.iandbrown.home_energy.networking.OctopusApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeterRepository(
    private val api: OctopusApi,
    private val usageDao: UsageDao
) {
    suspend fun syncConsumption(meters: List<Meter>) {
        usageDao.deleteAll()
        for (meter in meters) {
            var url: String? = null
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX]")
            do {
                val response = api.getConsumption(meter, url)
                response.results.forEach { dto ->
                    val startInstant = LocalDateTime.parse(dto.intervalStart, formatter)
                    val endInstant = LocalDateTime.parse(dto.intervalEnd, formatter)
                    var count = 1

                    while (startInstant.plusMinutes((30 * count).toLong()) < endInstant) {
                        ++count
                    }
                    if (count > 1) {
                        println("Multiple intervals: $count consumption ${dto.consumption}")
                    }
                    for (i in 0 until count) {
                        val period = (startInstant.hour * 2 + startInstant.minute / 30 + 30 * i).toShort()
                        usageDao.insert(
                            Usage(startInstant.year.toShort(),
                                startInstant.monthValue.toShort(),
                                startInstant.dayOfMonth.toShort(),
                                period,
                                meter.meterPointAdminNumber,
                                dto.consumption / count))
                    }
                }
                url = response.next
            } while (url != null)
        }
    }
}
