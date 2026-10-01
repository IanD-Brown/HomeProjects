package io.github.iandbrown.home_energy.repository

import io.github.iandbrown.home_energy.database.Meter
import io.github.iandbrown.home_energy.database.RawUsage
import io.github.iandbrown.home_energy.database.RawUsageDao
import io.github.iandbrown.home_energy.database.SettingDao
import io.github.iandbrown.home_energy.networking.OctopusApi
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class MeterRepository(
    private val api: OctopusApi,
    private val rawUsageDao: RawUsageDao,
    private val settingDao: SettingDao
) {
    suspend fun syncConsumption(meters: List<Meter>) {
        val datePattern = "(\\d{4})(-)(\\d{2})(-)(\\d{2})(T)(\\d{2})(:)(\\d{2})".toRegex()
        for (meter in meters) {
            var url: String? = null
            var counter = 0
            val startDateTime = getStartDateTime(meter.id)

            println("Fetching consumption for ${meter.name} (${meter.meterPointAdminNumber}) from $startDateTime")
            do {
                val response = api.getConsumption(meter, url, startDateTime)
                response.results.forEach { dto ->
                    if (dto.consumption != 0.0 && datePattern.containsMatchIn(dto.intervalStart)) {
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
                        for (period in startInstant..<endInstant) {
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

    private suspend fun getStartDateTime(meter: Int) : String {
        var lastYear: Short? = null
        var lastMonth : Short? = null
        var lastDay : Short? = null
        var lastPeriod : Short? = null
        rawUsageDao.get(meter).forEach {
            if (lastYear == null || it.year > lastYear) {
                lastYear = it.year
                lastMonth = null
                lastDay = null
                lastPeriod = null
            }
            if (it.year == lastYear && (lastMonth == null || it.month > lastMonth)) {
                lastMonth = it.month
                lastDay = null
                lastPeriod = null
            }
            if (it.year == lastYear && it.month == lastMonth && (lastDay == null || it.day > lastDay)) {
                lastDay = it.day
                lastPeriod = null
            }
            if (it.year == lastYear && it.month == lastMonth && it.day == lastDay && (lastPeriod == null || it.period > lastPeriod)) {
                lastPeriod = it.period
            }
        }

        return if ( lastPeriod != null) {
            val lastDateTime =
                LocalDateTime(lastYear!!.toInt(),lastMonth!!.toInt(), lastDay!!.toInt(), hour = lastPeriod / 2, minute = (lastPeriod % 2) * 30)
            val timeZone = TimeZone.currentSystemDefault()

            val instant = lastDateTime.toInstant(timeZone)
            val laterInstant = instant.plus(30, DateTimeUnit.MINUTE)
            val startDT = laterInstant.toLocalDateTime(timeZone)
            "${startDT.year}-${str(startDT.month.ordinal + 1)}-${str(startDT.day)}T${str(startDT.hour)}:${str(startDT.minute)}:00"
        } else {
            val year = when (settingDao.get()[0].fromYear) {
                0.toShort() -> 2023
                else -> settingDao.get()[0].fromYear
            }
            "$year-01-01T00:00:00"
        }
    }

    private fun str(value: Int, places: Int = 2) = value.toString().padStart(places, '0')

    internal suspend fun clearConsumption() {
        rawUsageDao.deleteAll()
    }
}

private fun toPeriod(parts: List<String>) : Short = (parts[7].toInt() * 2 + parts[9].toInt() / 30).toShort()
